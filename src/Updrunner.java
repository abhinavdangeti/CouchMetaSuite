package src;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import net.spy.memcached.internal.OperationFuture;

import com.couchbase.client.CouchbaseMetaClient;
import com.couchbase.client.MetaData;

public class Updrunner {

    public static void upds (Stronghold sh, CouchbaseMetaClient _sclient, CouchbaseMetaClient _dclient, String prefix) 
        throws JSONException, InterruptedException, ExecutionException {
        /*
         * Module to update items through updrms' on the source cluster,
         * and with the retrieved metaData, runs setwithmetas' on the
         * destination cluster
         */
        ArrayList<DelayedOps> delayedupds = new ArrayList<DelayedOps>();
        Random gen = new Random ( 987654321 );
        StringBuffer value = new StringBuffer();
        String CHAR_LIST = "ABCD";
        while (value.length() < sh.getItemsize()) {
            value.append(CHAR_LIST);
        }

        List<OperationFuture<MetaData>> creates = new LinkedList<OperationFuture<MetaData>>();
        for (int i=(int) (sh.getItemcount() + sh.getAddCount() - Math.round(1 - sh.getUpdRatio())); i<(sh.getItemcount() + sh.getAddCount()); i++) {
            OperationFuture<MetaData> updrm = null;
            OperationFuture<Boolean> updm = null;
            String key = String.format("%s%d", prefix, i);
            if (sh.isJson()) {
                JSONObject _val = Spawner.retrieveJSON(gen, sh.getItemsize());
                updrm = _sclient.setReturnMeta(key, sh.getExpiration(), 0, _val.toString());
                assert(updrm.get() != null);
                if (sh.getdoVerify())
                    sh.storeinSTable(key, _val.toString(), updrm.get());
                if (sh.getReplicationFlag()) {
                    delayedupds.add(new DelayedOps(key, _val.toString(), updrm.get()));
                } else {
                    try {
                        updm = _dclient.setWithMeta(key, _val.toString(), updrm.get(), 0);
                    } catch (Exception e) {
                        System.out.println("Update failed at destination, either because MetaData wasn't retreived from setrm");
                        if (updm.get().booleanValue() == false)
                            System.out.println("Reason: " + updm.getStatus().getMessage());
                    }
                    assert(updm.get().booleanValue());
                    if (sh.getdoVerify())
                        sh.storeinDTable(key, _val.toString(), null);
                }
            } else {
                updrm = _sclient.setReturnMeta(key, sh.getExpiration(), 0, value.toString());
                assert(updrm.get() != null);
                if (sh.getdoVerify())
                    sh.storeinSTable(key, value.toString(), updrm.get());
                if (sh.getReplicationFlag()) {
                    delayedupds.add(new DelayedOps(key, value.toString(), updrm.get()));
                } else {
                    try {
                        updm = _dclient.setWithMeta(key, value.toString(), updrm.get(), 0);
                    } catch (Exception e) {
                        System.out.println("Update failed at destination, either because MetaData wasn't retreived from setrm");
                        if (updm.get().booleanValue() == false)
                            System.out.println("Reason: " + updm.getStatus().getMessage());
                    }
                    assert(updm.get().booleanValue());
                    if (sh.getdoVerify())
                        sh.storeinDTable(key, value.toString(), null);
                }
            }
            creates.add(updrm);
        }

        if (sh.getReplicationFlag()) {
            System.out.println("Wait for 10 seconds, before sending updMetas");
            Thread.sleep(10000);
            for (DelayedOps d : delayedupds) {
                OperationFuture<Boolean> updm = _dclient.setWithMeta(d.getkey(), d.getval(), d.getmeta(), 0);
                assert(updm.get().booleanValue());
                if (sh.getdoVerify())
                    sh.storeinDTable(d.getkey(), d.getval(), null);
            }
        }

        while (!creates.isEmpty()) {
            if (creates.get(0).isDone() == false){
                System.err.println("Update failed");
                continue;
            }
            creates.remove(0);
        }
    }
}
