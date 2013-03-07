
import groovy.json.*

def repos = new JsonSlurper().parseText("https://api.github.com/orgs/Netflix/repos".toURL().text)
def projectWhitelist = ['RxJava']
repos.findAll { projectWhitelist.contains(it.name) }.each { repo ->
    println "$repo.name $repo.url"
    // Trunk build
    job {
        name "${repo.name}-master"
        description ellipsize(repo.description, 255)
        logRotator(60,-1,-1,20)
        scm {
            git(repo.git_url, 'master')
        }
        jdk('Sun JDK 1.6 (latest)')
        configure { project ->
            project / triggers / 'com.cloudbees.jenkins.GitHubPushTrigger'(plugin:'github@1.5') / spec {
            }
        }
        configure { project ->
            project / 'properties' / 'nectar.plugins.rbac.groups.JobProxyGroupContainer'(plugin:'nectar-rbac@3.4') / groups {
            }
        }
        steps {
            gradle('clean build')
        }
    }

    // Pull request build
    // Should be similar to above, but it's probably more readable to just repeat the code.
    job {
        name "${repo.name}-pullrequest"
        description ellipsize(repo.description, 255)
        logRotator(60,-1,-1,20)
        scm {
            git(repo.git_url, 'master')
        }
        jdk('Sun JDK 1.6 (latest)')
        configure { project -> 
            project / triggers / 'com.cloudbees.jenkins.GitHubPushTrigger'(plugin:'github@1.5') / spec {
            }
        }
        // TBD CloudBees pull request plugin
        steps {
            gradle('clean test')
        }
    }
}

String ellipsize(String input, int maxLength) {
  if (input == null || input.length() < maxLength) {
    return input;
  }
  return input.substring(0, maxLength) + "...";
}
