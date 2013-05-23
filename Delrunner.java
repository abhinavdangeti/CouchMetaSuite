import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import net.spy.memcached.internal.OperationFuture;

import com.couchbase.client.CouchbaseMetaClient;
import com.couchbase.client.MetaData;

public class Delrunner {

    public static void dels (Stronghold sh, CouchbaseMetaClient _sclient, CouchbaseMetaClient _dclient) 
	throws InterruptedException, ExecutionException {
	    /*
	     * Method to delete items through delrms' on the source cluster,
	     * and with the retrieved metaData runs delwithmetas' on the
	     * destination cluster
	     */
	    ArrayList<DelayedOps> delayeddels = new ArrayList<DelayedOps>();
	    for (int i=0; i<(int)((double)sh.getItemcount() * sh.getDelRatio()); i++) {
		OperationFuture<MetaData> delrm = null;
		OperationFuture<Boolean> delm = null;
		String key = String.format("%s%d", sh.getPrefix(), i);
		delrm = _sclient.deleteReturnMeta(key, 0);
		assert(delrm.get() != null);
		//	    sh.storeinSTable(key, null, delrm.get());
		if (sh.getReplicationFlag()) {
		    delayeddels.add(new DelayedOps(key, null, delrm.get()));
		} else {
		    try {
			delm = _dclient.deleteWithMeta(key, delrm.get(), 0);
		    } catch (Exception e) {
			System.out.println("Delete failed at destination, either because MetaData wasn't retreived from setrm");
			if (delm.get().booleanValue() == true)
			    System.out.println("Reason: " + delm.getStatus().getMessage());
		    }
		    assert(delm.get().booleanValue());
		    //			if (delm.get().booleanValue())
		    //		    	sh.storeinDTable(key, null, null);
		}
	    }

	    if (sh.getReplicationFlag()) {
		System.out.println("Wait for 10 seconds, before sending delMetas");
		Thread.sleep(10000);
		for (DelayedOps d : delayeddels) {
		    OperationFuture<Boolean> delm = _dclient.deleteWithMeta(d.getkey(), d.getmeta(), 0);
		    assert(delm.get().booleanValue());
		    //	    		if (delm.get().booleanValue())
		    //	    			sh.storeinDTable(d.getkey(), null, null);
		}
	    }
	}
}
