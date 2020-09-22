def file = new File( request.getOutputDirectory(), request.getArtifactId() + "/src/test/resources/kill-auth-server.sh" );
file.setExecutable(true, false);
