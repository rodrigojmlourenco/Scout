package pt.ulisboa.tecnico.cycleourcity.scout.action;

import edu.mit.media.funf.config.Configurable;
import edu.mit.media.funf.util.LogUtil;
import pt.ulisboa.tecnico.cycleourcity.scout.datasource.Startable;

import android.util.Log;

public class StartDataSourceAction extends Action implements Startable.TriggerAction {
    
    @Configurable
    private Startable target = null;
    
    StartDataSourceAction() {
    }

    public void setTarget(Startable target) {
        this.target = target;
    }
    
    protected void execute() {
        if (target == null) 
            return;
        Log.d(LogUtil.TAG, "running probe action start");
        target.start();
    }
}
