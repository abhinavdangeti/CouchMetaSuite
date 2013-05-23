COUCHBASE_META_OPERATION implementation:

    Specify cluster-load details in test.properties
                               
    source/destination:             ...source-node(s), seperated by ","
    source-port/destination-port:   ...couchbase port
    bucket-name:                    ...name of bucket
    bucket-password:                ...bucket's password
    bucket-memQuota:                ...bucket's memQuota (automation not yet) 
    json=false:                     ...doesn't spawn JSON values
    item-count:                     ...no. of sets
    item-size:                      ...size of each item (approximation) 
    prefix:                         ...prefix of generated keys 
    exp-ratio:                      ...ratio of item-count to be expired 
    expiration-time:                ...expirtaion time
    del-ratio:                      ...ratio of item-count to be deleted (disjoint with exp-ratio)
    add-count:                      ...items to add
    replication-starts-first:       ...false => setm immediaetly after setrm
                                       true => setms run after all setrms + timed_wait (10s)

ORDER OF EXECUTION:

    - All the sets
    - All the deletes
    - All the adds
    - Verification (To be implemented)

COMPILE / EXECUTION Instructions:

    To compile:
    make compile

    To execute:
    make run

    To clean Class files:
    make clean

FILES:

    Helper                          ...Parent class that orchestrates all the operations
    Setrunner                       ...Class that runs sets, be it with expires or not, will populate
                                       hashtables used in verification
    Addrunner                       ...Class that runs adds, be it with expires or not, will populate
                                       hashtables used in verification
    Delrunner                       ...Class that runs deletes, will populate hashtables used in 
                                       verification, del-ratio works on item-count for now
    
    Stronghold                      ...Stores attributes retrieved from test.properties, hashtables
                                       used in verification declared here
    Hashstructure                   ...Hashtables (used in verification) will have key as the item's
                                       key, and value as an instance of this class (which contains 
                                       the document and metadata)
    DelayedOps                      ...In case of replication-starts-first flag set to true, an arraylist
                                       with data being an instance of this class (which contains key, 
                                       value, metadata) is populated for setWithMeta commands to run once
                                       all the setReturnMetas complete + a timeout threshold of 10s.
    
    Spawner                         ...Creates JSON documents if json set to true in test.properties
    ClusterSetup                    ...To setup the cluster based on specifications (NOT IN USE)
