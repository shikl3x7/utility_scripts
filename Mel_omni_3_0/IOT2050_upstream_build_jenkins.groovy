pipeline {
    agent { label 'Omni_3_0_Build_System' }
    parameters {
        string(name: 'PROJECT_DIR', defaultValue: ' ' , description: 'Path where OMNI 3.0 builds happen for Industrial _C(IPC)')
        string(name: 'DISTRO', defaultValue: 'mel-omni' , description: 'Build images specific to distro')
        string(name: 'MACHINE', defaultValue: 'iot2050' , description: 'Name of the Machine')
        string(name: 'BUILD_IMAGES', defaultValue: 'service-stick-image development-ade' , description: 'Images to be built')
        string(name: 'INPFTP_URI', defaultValue: 'inpftp@inpftp.ina.mentorg.com:/home/inpftp/pub/OMNI_3.0_GA_UPSTREAM/iot2050' , description: 'Location where build artifacts are copied to')
        string(name: 'EMAIL_LIST_FAIL', defaultValue: 'shivaschandra_kl@mentor.com', description: 'Email recepients')
    }
    stages {
        stage('Init') {
            steps {
                script {
                    // load parameters into regular variables
                    MACHINE                 = "${params.MACHINE}"
                    PROJECT_DIR             = "${env.WORKSPACE}"
                    DISTRO                  = "${params.DISTRO}"
                    BUILD_IMAGES            = "${params.BUILD_IMAGES}"
                    SERVICE_STICK_IMAGES    = "${params.SERVICE_STICK_IMAGES}"
                    INPFTP_URI              = "${params.INPFTP_URI}"
                }
            }  
        }        
        stage('Checkout') {
            steps {
                checkout changelog: true, poll: false, scm: [
                $class: 'RepoScm',
                currentBranch: true,
                forceSync: true,
                noTags: true,
                manifestBranch: 'master',
                manifestRepositoryUrl: 'git@github.com:MentorEmbedded/industrial-manifest',
                manifestFile: 'prod/3.0/all.xml',
                quiet: true
                ]
            }
        }        
        stage ('Build') {
            steps {
                script {
                    sh '''#!/bin/bash
                        # abort on error
                        set -e
                        echo "\$MACHINE"
                        PROJECT_DIR=$(pwd)
                        BUILD_PATH="${PROJECT_DIR}/BUILD-\$MACHINE"
                        echo "\$BUILD_PATH"
                        mkdir -p \${BUILD_PATH}
                        echo "\$MACHINE"
                        IMAGE_ID="$(date +%Y)_$(date +%m)_$(date +%d)_BUILD${BUILD_ID}"
                        echo "$IMAGE_ID"
                        echo -e "\n### Removing builds in build folder to keep save sapce in build system ###\n"
                        echo "manas" | sudo rm -rf $(ls -d -1t \${BUILD_PATH}/* | tail -n +2)
                        # Install debian packages
                        echo "manas" | sudo -S ${PROJECT_DIR}/industrial-core/scripts/setup-debian
                        
						# Build from upstream source
						# echo 'INDUSTRIAL_SOURCES="upstream"' >> conf/local.conf

                        # Source the setup-environment for supported machines
                        
                        . ${PROJECT_DIR}/industrial-core/setup-environment -d ${DISTRO} -b \${BUILD_PATH}/\${IMAGE_ID} ${MACHINE}
                        #ln -s ${PROJECT_DIR}/../cache cache
                        #ln -s ${PROJECT_DIR}/../downloads downloads
                        # Build from upstream source
                        echo 'MEL_APT_SOURCES="upstream"' >> \${BUILD_PATH}/\${IMAGE_ID}/conf/local.conf
                        echo -e 'IMAGE_FEATURES_append = " mtda "' >> \${BUILD_PATH}/\${IMAGE_ID}/conf/local.conf
                        #echo 'MEL_USE_CACHE="1"' >> \${BUILD_PATH}/\${IMAGE_ID}/conf/local.conf
                        # Build Images
                        for image in ${BUILD_IMAGES} ; do
	                        echo -e "\n#### Building \${image} ####\n"
                            bitbake \${image}
                        done
                        echo -e "\n### Removing build folder in ipftp to keep last 3 stable builds only ####\n"
                        sshpass -p "inpftp" ssh inpftp@inpftp.ina.mentorg.com 'rm -rf $(ls -d -1t /home/inpftp/pub/OMNI_3.0_GA_UPSTREAM/iot2050/* | tail -n +2)'
                        echo -e "\n#### Copying service-stick-image and ADE to inpftp server ####\n"
                        # Copy service-stick-image to inpftp server
                        sshpass -p "inpftp" rsync tmp/deploy/images/iot2050/service-stick-image-mel-omni-iot2050.wic.img \${INPFTP_URI}/\${IMAGE_ID}/
                        sshpass -p "inpftp" rsync tmp/deploy/images/iot2050/development-image-mel-omni-iot2050.wic.img \${INPFTP_URI}/\${IMAGE_ID}/
                        sshpass -p "inpftp" rsync tmp/deploy/ade/*.zip \${INPFTP_URI}/\${IMAGE_ID}/
                        sshpass -p "inpftp" rsync tmp/deploy/images/iot2050/*.manifest \${INPFTP_URI}/\${IMAGE_ID}/
                    '''
                }
            }
        }
       		
    }
     post { 
        failure {
                emailext attachLog: true,
                subject: "Failed Pipeline: ${currentBuild.fullDisplayName}",
                body: '''${SCRIPT, template="build_failure.groovy"}''',
                to: "${EMAIL_LIST_FAIL}"            
        }
    }
}
