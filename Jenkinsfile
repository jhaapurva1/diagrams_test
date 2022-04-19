@Library('devops-lib') _
def params = ["repo_name":"campaign-performance",
              "build_tool":"maven",
              "maintainer":"vineet.yadav",
              "branch_params":[
                "develop":[
                  "push_to_jfrog": true,
                  "push_to_s3": true,
                  "skip_test": true,
                  "skip_sonar": false,
                  "skip_security_scan": false,
                  "notify_channel": "supplier-ads"
                 ],
                "master":[
                   "push_to_jfrog": true,
                   "push_to_s3": true,
                   "skip_test": true,
                   "skip_sonar": false,
                   "skip_security_scan": false,
                   "notify_channel": "supplier-ads"
                ]
              ]
             ]

buildPipeline(params)