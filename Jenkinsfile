@Library('devops-lib') _
def params = ["repo_name":"campaign-performance",
              "build_tool":"maven",
              "maintainer":"vineet.yadav",
              "skip_test": true,
              "notify_channel": "supplier-ads-alerts",
              "branch_params":[
                "develop":[
                  "push_to_jfrog": true,
                  "push_to_s3": true
                ]
              ]
             ]

buildPipeline(params)