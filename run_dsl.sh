DSL_JAR=$JOB_DSL_HOME/job-dsl-core/build/libs/job-dsl-core-*-SNAPSHOT-standalone.jar
java -jar $DSL_JAR netflixoss.dsl.groovy $@
