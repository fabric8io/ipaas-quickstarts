Heapster enables monitoring of Clusters using [cAdvisor](https://github.com/google/cadvisor).

Heapster supports [Kubernetes](https://github.com/GoogleCloudPlatform/kubernetes) natively and collects resource usage of all the Pods running in the cluster. It was built to showcase the power of core Kubernetes concepts like labels and pods and the awesomeness that is cAdvisor. 

Heapster can be used to enable cluster wide monitoring on other Cluster management solutions by running a simple cluster specific buddy container that will help heapster with discovery of hosts. For example, take a look at [this guide](clusters/coreos/README.md) for setting up Cluster monitoring in [CoreOS](https://coreos.com).

#####How heapster works on Kubernetes:
1. Discovers all minions in a Kubernetes cluster
2. Collects container statistics from the cadvisors running on the minions
2. Organizes stats into Pods
3. Stores Pod stats in a configurable backend

Along with each container stat entry, it's Pod ID, Container name, Pod IP, Hostname and Labels are also stored. Labels are stored as key:value pairs.

Heapster currently supports in-memory and [InfluxDB](http://influxdb.com) backends. Patches are welcome for adding more storage backends.
