
import groovy.json.*

// Get GitHub repo name from the parameters for this build
def thr = Thread.currentThread()
def build = thr?.executable
def resolver = build.buildVariableResolver
def projectToBuild = resolver.resolve('project')

def repos = new JsonSlurper().parseText("https://api.github.com/orgs/Netflix/repos".toURL().text)
def projectWhitelist = ["${projectToBuild}"]
repos.findAll { projectWhitelist.contains(it.name) }.each { repo ->
    println "$repo.name $repo.url"
    // Trunk build
    job {
        name "${repo.name}-master"
        description ellipsize(repo.description, 255)
        logRotator(60,-1,-1,20)
        timeout(20)
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
            project / 'properties' / 'com.cloudbees.jenkins.plugins.PublicKey'(plugin:'cloudbees-public-key@1.1') {
            }
        }
        steps {
            gradle('clean build')
        }
        publishers {
            archiveJunit('**/build/test-results/TEST*.xml')
        }
    }

    // Pull request build
    // Should be similar to above, but it's probably more readable to just repeat the code.
    job {
        name "${repo.name}-pullrequest"
        description ellipsize(repo.description, 255)
        logRotator(60,-1,-1,20)
        timeout(20)
        scm {
            git(repo.git_url, 'master')
        }
        jdk('Sun JDK 1.6 (latest)')
        configure { project -> 
            project / triggers / 'com.cloudbees.jenkins.plugins.github__pull.PullRequestBuildTrigger'(plugin:'github-pull-request-build@1.0-beta-2') / spec {
            }
        }
        configure { project ->
            project / 'properties' / 'nectar.plugins.rbac.groups.JobProxyGroupContainer'(plugin:'nectar-rbac@3.4') / groups {
            }
            project / 'properties' / 'com.cloudbees.jenkins.plugins.PublicKey'(plugin:'cloudbees-public-key@1.1') {
            }
            project / 'properties' / 'com.cloudbees.jenkins.plugins.git.vmerge.JobPropertyImpl'(plugin:'git-validated-merge@3.6') / postBuildPushFailureHandler(class:'com.cloudbees.jenkins.plugins.git.vmerge.pbph.PushFailureIsFailure') 
            project / 'properties' / 'com.coravy.hudson.plugins.github.GithubProjectProperty'(plugin:'github@1.5') / projectUrl('https://github.com/Netflix/RxJava/')
        }
        steps {
            gradle('clean build')
        }
        publishers {
            archiveJunit('**/build/test-results/TEST*.xml')
        }
    }
}

String ellipsize(String input, int maxLength) {
  if (input == null || input.length() < maxLength) {
    return input;
  }
  return input.substring(0, maxLength) + '...';
}
