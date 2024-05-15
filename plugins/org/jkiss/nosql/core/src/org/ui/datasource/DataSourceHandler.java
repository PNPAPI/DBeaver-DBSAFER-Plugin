/*
 * Nosql - Universal Database Manager
 */
package org.jkiss.ui.actions.datasource;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.ISaveablePart;
import org.eclipse.ui.progress.UIJob;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.NosqlPreferences;
import org.jkiss.Log;
import org.jkiss.ModelPreferences;
import org.jkiss.core.CoreFeatures;
import org.jkiss.model.DBPDataSource;
import org.jkiss.model.DBPDataSourceContainer;
import org.jkiss.model.DBPDataSourceTask;
import org.jkiss.model.DBUtils;
import org.jkiss.model.connection.DBPConnectionConfiguration;
import org.jkiss.model.exec.*;
import org.jkiss.model.preferences.DBPPreferenceStore;
import org.jkiss.model.qm.QMUtils;
import org.jkiss.model.runtime.DBRProgressListener;
import org.jkiss.model.runtime.DBRProgressMonitor;
import org.jkiss.model.runtime.DBRRunnableWithProgress;
import org.jkiss.model.runtime.VoidProgressMonitor;
import org.jkiss.model.struct.DBSInstance;
import org.jkiss.registry.DataSourceDescriptor;
import org.jkiss.runtime.DBWorkbench;
import org.jkiss.runtime.jobs.ConnectJob;
import org.jkiss.runtime.jobs.DisconnectJob;
import org.jkiss.ui.NosqlIcons;
import org.jkiss.ui.UIIcon;
import org.jkiss.ui.UIUtils;
import org.jkiss.ui.actions.DataSourceHandlerUtils;
import org.jkiss.ui.dialogs.ConfirmationDialog;
import org.jkiss.ui.editors.entity.handlers.SaveChangesHandler;
import org.jkiss.utils.RuntimeUtils;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.rest.RestClient;

import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.Map;

public class DataSourceHandler {
    public static final int END_TRANSACTION_WAIT_TIME = 3000;
    private static final Log log = Log.getLog(DataSourceHandler.class);

    /**
     * Connects datasource
     *
     * @param monitor             progress monitor or null. If nul then new job will be started
     * @param dataSourceContainer container to connect
     * @param onFinish            finish handler
     */
    public static void connectToDataSource(@Nullable DBRProgressMonitor monitor,@NotNull DBPDataSourceContainer dataSourceContainer,@Nullable final DBRProgressListener onFinish) {
        if (dataSourceContainer instanceof final DataSourceDescriptor dataSource && !dataSourceContainer.isConnected()) {
            Job[] connectJobs = Job.getJobManager().find(dataSource);
            if (!ArrayUtils.isEmpty(connectJobs)) {
                // Already connecting/disconnecting - just return
                if (monitor != null && connectJobs.length == 1) {
                    try {
                        connectJobs[0].join(0, monitor.getNestedMonitor());
                    } catch (InterruptedException e) {
                        log.debug(e);
                    }
                }
                return;
            }

            // Ask for additional credentials if needed
            if (!DataSourceHandlerUtils.resolveSharedCredentials(dataSource, onFinish)) {
                return;
            }

            CoreFeatures.CONNECTION_OPEN.use(Map.of("driver", dataSourceContainer.getDriver().getPreconfiguredId()));
            final ConnectJob connectJob = new ConnectJob(dataSource);

            // Pnp Db Access Checking
            if (checkDbAccess(dataSource, connectJob)) return;

            final JobChangeAdapter jobChangeAdapter = new JobChangeAdapter() {
                @Override
                public void done(IJobChangeEvent event) {
                    IStatus result = connectJob.getConnectStatus();
                    if (onFinish != null) {
                        onFinish.onTaskFinished(result);
                    } else if (!result.isOK()) {
                        DBWorkbench.getPlatformUI().showError(connectJob.getName(), null, result);
                    }
                }
            };
            if (monitor != null) {
                connectJob.runSync(monitor);
                jobChangeAdapter.done(new IJobChangeEvent() {
                    @Override
                    public long getDelay() {
                        return 0;
                    }

                    @Override
                    public Job getJob() {
                        return connectJob;
                    }

                    @Override
                    public IStatus getResult() {
                        return connectJob.getConnectStatus();
                    }

                    public IStatus getJobGroupResult() {
                        return null;
                    }
                });
            } else {
                connectJob.addJobChangeListener(jobChangeAdapter);
                // Schedule in UI because connect may be initiated during application startup
                // and UI is still not initiated. In this case no progress dialog will appear
                // to be sure run in UI async
                UIUtils.asyncExec(connectJob::schedule);
            }
        }
    }

    public static void disconnectDataSource(DBPDataSourceContainer dataSourceContainer, @Nullable final Runnable onFinish) {

        // Save users
        for (DBPDataSourceTask user : dataSourceContainer.getTasks()) {
            if (user instanceof ISaveablePart) {
                if (!SaveChangesHandler.validateAndSave(new VoidProgressMonitor(), (ISaveablePart) user)) {
                    return;
                }
            }
        }
        if (!checkAndCloseActiveTransaction(dataSourceContainer, false)) {
            return;
        }

        if (dataSourceContainer instanceof final DataSourceDescriptor dataSourceDescriptor && dataSourceContainer.isConnected()) {
            if (!ArrayUtils.isEmpty(Job.getJobManager().find(dataSourceDescriptor))) {
                // Already connecting/disconnecting - just return
                return;
            }

            CoreFeatures.CONNECTION_CLOSE.use(Map.of(
                    "driver", dataSourceContainer.getDriver().getPreconfiguredId()
            ));

            final DisconnectJob disconnectJob = new DisconnectJob(dataSourceDescriptor);
            disconnectJob.addJobChangeListener(new JobChangeAdapter() {
                @Override
                public void done(IJobChangeEvent event) {
                    IStatus result = disconnectJob.getConnectStatus();
                    if (onFinish != null) {
                        onFinish.run();
                    } else if (result != null && !result.isOK()) {
                        DBWorkbench.getPlatformUI().showError(disconnectJob.getName(), null, result);
                    }
                    //DataSourcePropertyTester.firePropertyChange(DataSourcePropertyTester.PROP_CONNECTED);
                }
            });
            disconnectJob.schedule();
        }
    }

    public static void reconnectDataSource(final DBRProgressMonitor monitor, final DBPDataSourceContainer dataSourceContainer) {
        disconnectDataSource(dataSourceContainer, () ->
                connectToDataSource(monitor, dataSourceContainer, null));
    }

    public static boolean checkAndCloseActiveTransaction(DBPDataSourceContainer container, boolean isReconnect) {
        try {
            DBPDataSource dataSource = container.getDataSource();
            if (dataSource == null) {
                return true;
            }

            for (DBSInstance instance : dataSource.getAvailableInstances()) {
                if (!checkAndCloseActiveTransaction(instance.getAllContexts(), isReconnect)) {
                    return false;
                }
            }
        } catch (Throwable e) {
            log.debug(e);
        }
        return true;
    }

    public static boolean checkAndCloseActiveTransaction(DBCExecutionContext[] contexts, boolean isReconnect) {
        if (contexts == null) {
            return true;
        }

        Boolean commitTxn = null;
        for (final DBCExecutionContext context : contexts) {
            // First rollback active transaction
            try {
                if (QMUtils.isTransactionActive(context)) {
                    if (commitTxn == null) {
                        // Ask for confirmation
                        TransactionCloseConfirmer closeConfirmer = new TransactionCloseConfirmer(
                                context.getDataSource().getContainer().getName() + " (" + context.getContextName() + ")",
                                isReconnect
                        );
                        UIUtils.syncExec(closeConfirmer);
                        switch (closeConfirmer.result) {
                            case IDialogConstants.YES_ID:
                                commitTxn = true;
                                break;
                            case IDialogConstants.NO_ID:
                                commitTxn = false;
                                break;
                            default:
                                return false;
                        }
                    }
                    final boolean commit = commitTxn;
                    UIUtils.runInProgressService(monitor -> closeActiveTransaction(monitor, context, commit));
                }
            } catch (Throwable e) {
                log.warn("Can't rollback active transaction before disconnect", e);
            }
        }
        return true;
    }

    public static void closeActiveTransaction(DBRProgressMonitor monitor, DBCExecutionContext context, boolean commitTxn) {
        monitor.beginTask("Close active transaction", 1);
        try (DBCSession session = context.openSession(monitor, DBCExecutionPurpose.UTIL, "End active transaction")) {
            // Disable logging to avoid commit mode recovery and other UI callbacks
            session.enableLogging(false);
            monitor.subTask("End active transaction");
            EndTransactionTask task = new EndTransactionTask(session, commitTxn);
            RuntimeUtils.runTask(task, "Close active transactions", END_TRANSACTION_WAIT_TIME);
        } finally {
            monitor.done();
        }
    }

    public static boolean confirmTransactionsClose(DBCExecutionContext[] contexts) {
        if (contexts.length == 0) {
            return false;
        }
        TransactionEndConfirmer closeConfirmer = new TransactionEndConfirmer(contexts[0].getDataSource());
        UIUtils.syncExec(closeConfirmer);
        return closeConfirmer.result;
    }

    private static class EndTransactionTask implements DBRRunnableWithProgress {
        private final DBCSession session;
        private final boolean commit;

        private EndTransactionTask(DBCSession session, boolean commit) {
            this.session = session;
            this.commit = commit;
        }

        @Override
        public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
            DBCTransactionManager txnManager = DBUtils.getTransactionManager(session.getExecutionContext());
            if (txnManager != null) {
                try {
                    if (commit) {
                        txnManager.commit(session);
                    } else {
                        txnManager.rollback(session, null);
                    }
                } catch (DBCException e) {
                    throw new InvocationTargetException(e);
                }
            }
        }
    }

    private static class TransactionCloseConfirmer implements Runnable {
        final String name;
        final boolean isReconnect;
        int result = IDialogConstants.NO_ID;

        private TransactionCloseConfirmer(String name, boolean isReconnect) {
            this.name = name;
            this.isReconnect = isReconnect;
        }

        @Override
        public void run() {
            result = ConfirmationDialog.confirmAction(
                    null,
                    this.isReconnect ? NosqlPreferences.CONFIRM_TXN_RECONNECT : NosqlPreferences.CONFIRM_TXN_DISCONNECT,
                    ConfirmationDialog.QUESTION_WITH_CANCEL,
                    name);
        }
    }

    private static class TransactionEndConfirmer implements Runnable {
        final DBPDataSource dataSource;
        boolean result;

        private TransactionEndConfirmer(DBPDataSource dataSource) {
            this.dataSource = dataSource;
        }

        @Override
        public void run() {
            Display.getCurrent().beep();
            TransactionEndConfirmDialog dialog = new TransactionEndConfirmDialog(dataSource);
            result = dialog.open() == IDialogConstants.OK_ID;
        }
    }

    static class TransactionEndConfirmDialog extends MessageDialog {

        private int countdown = 10;

        public TransactionEndConfirmDialog(DBPDataSource dataSource) {
            super(UIUtils.getActiveShell(),"End transaction",NosqlIcons.getImage(UIIcon.TXN_ROLLBACK),"Transactions in database '" + dataSource.getName() + "' will be ended because of the long idle period." +"\nPress '" + IDialogConstants.CANCEL_LABEL + "' to prevent this.",
                    MessageDialog.WARNING,
                    null,
                    0);
        }

        @Override
        public int open() {
            UIJob countdownJob = new UIJob("Confirmation countdown") {
                {
                    setUser(false);
                    setSystem(true);
                }
                @Override
                public IStatus runInUIThread(IProgressMonitor monitor) {
                    Shell shell = getShell();
                    if (shell == null || shell.isDisposed()) {
                        return Status.OK_STATUS;
                    }
                    Button okButton = getButton(IDialogConstants.OK_ID);
                    if (okButton != null) {
                        okButton.setText(IDialogConstants.OK_LABEL + " (" + countdown + ")");
                    }

                    countdown--;
                    if (countdown <= 0) {
                        UIUtils.asyncExec(() -> close());
                    } else {
                        schedule(1000);
                    }
                    return Status.OK_STATUS;
                }
            };
            countdownJob.schedule(100);

            return super.open();
        }

        @Override
        protected void createButtonsForButtonBar(Composite parent) {
            Button okButton = createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, false);
            Button cancelButton = createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, true);
            setButtons(okButton, cancelButton);
        }
    }

    /**
     * PNP DB Access Control Results
     * @param dataSource
     * @param connectJob
     * @return
     */
    private static boolean checkDbAccess(DataSourceDescriptor dataSource, ConnectJob connectJob) {
        // Added access control function for DB based on access information
        if(connectJob != null){
            try {
                DBPConnectionConfiguration currentConfiguration = dataSource.getConnectionConfiguration();

                if(currentConfiguration != null){
                    DbAccessDto dbAccessDto = new DbAccessDto(currentConfiguration.getHostName(),
                            currentConfiguration.getHostPort(),
                            currentConfiguration.getUserName(),
                            PnpUtil.getPreferenceStoreString(ModelPreferences.PNP_LOGIN_ID_STORE),
                            currentConfiguration.getDatabaseName(),
                            "com",
                            PnpUtil.getPreferenceStoreString(ModelPreferences.PNP_LOGIN_IP_ADDRESS));
                    final PnpController pnpInstance = RestClient
                            .builder(URI.create("http://localhost:22118/api"), PnpController.class)
                            .create();

                    PnpResult<DbAccessResult> dbAccessResult = pnpInstance.checkDbConnect(dbAccessDto);

                    if(dbAccessResult != null || dbAccessResult.getResult() == null || "false".equals(dbAccessResult.getResult())){
                        log.debug("PNP DBACCESS DENY");
                        String message = String.format("IP : %s \n Port : %s \n ID : %s",
                                currentConfiguration.getHostName(),
                                currentConfiguration.getHostPort(),
                                currentConfiguration.getUserName());
                        PnpUtil.showErrorDialog("Db connection blocked.", message);
                        return true;
                    }
                }
            } catch (Exception e){
                log.debug("ERROR Pnp DbAccess Check Fail");
                // Creating the Db Access Control API Call Fail dialog
                PnpUtil.showErrorDialog("Pnp DbAccess Check Fail", null);
                return true;
            }
        }
        return false;
    }

}
