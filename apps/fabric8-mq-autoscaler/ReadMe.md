The MQ autoscaler uses [kubernetes service](http://fabric8.io/v2/services.html), to monitor a group of MQ brokers,
matching a group of labels defined by ENV variables **AMQ_SERVICE_ID** (default is _fabricMQ_), and **AMQ_GROUP_NAME** (default is _default_)

The autoscaler will examine all MQ brokers run in the group defined by the **AMQ_GROUP_NAME** and spin up additional brokers, or remove them.
After changing the number of running MQ brokers, the auto scaler will request some clients reconnect, to balance the load.

Environment variables:

* **KUBERNETES_MASTER** (_"http://localhost:8080"_) location of the kubernetes master
 * **POLL_TIME** (_5_) - the time in seconds between polling the group to determine the load
* **MAX_GROUP_SIZE** (_2_) - the maximum number of brokers allowed in the group
* **MIN_GROUP_SIZE** (_1_) - the minimum number of brokers allowed in the group
* **MAX_BROKER_CONNECTIONS** (_10_) - the maximum number of connections allowed for a single broker
* **MAX_BROKER_DESTINATIONS** (_10_) - the maximum number of destinations allowed for a broker
*  **MAX_DESTINATION_DEPTH** (_10_) the maximum number of unconsumed messages on a broker before scaling up more brokers
*  **MAX_PRODUCERS_PER_DESTINATION** (_10_) - the maximum number of producers allowed per destination
*  **MAX_CONSUMERS_PER_DESTINATION** (_10_) - the maximum number of consumers allowed per destination

The auto scaler will also redistribute clients if destinations have no active consumers.

