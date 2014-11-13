## Fabric8 Quickstarts

This project contains the quickstarts for the [fabric8 project](http://fabric8.io/).


### Pushing docker images

If you wish to push docker images to a private or public registry you will need to add a section to your **~/.m2/settings.xml** file with a login/pwd:

```
	<servers>
       <server>
           <id>192.168.59.103:5000</id>
           <username>jolokia</username>
           <password>jolokia</password>
       </server>
        ...
  </servers>
```

For local containers they can be dummy login/passwords. For more details [see the docker maven plugin docs](https://github.com/rhuss/docker-maven-plugin/blob/master/doc/manual.md#authentication)
