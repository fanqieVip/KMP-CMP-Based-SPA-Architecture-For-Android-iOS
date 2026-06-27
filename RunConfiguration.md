Xcode + Android Studio构建环境同步配置

ios构建环境分别为iosApp-debug、iosApp-beta、iosApp-alpha、iosApp-release

<<<<<<<<<<<<<<<<<<<<< Android Studio配置 >>>>>>>>>>>>>>>>>>>>
在各自环境的Run/Debug Configurations中添加Before-launch脚本，并确保在Build脚本之前执行。以下是各个脚本的配置信息

###### iosApp-debug配置 ######
Name: GRADLE_ENV_DEBUG
Group: GRADLE_ENV
Description: GRADLE_ENV_DEBUG
Program: /bin/bash
Arguments: -c "sh $ProjectFileDir$/iosApp/env/env_config_debug.sh"
Working directory: $ProjectFileDir$

###### iosApp-beta配置 ######
Name: GRADLE_ENV_BETA
Group: GRADLE_ENV
Description: GRADLE_ENV_BETA
Program: /bin/bash
Arguments: -c "sh $ProjectFileDir$/iosApp/env/env_config_beta.sh"
Working directory: $ProjectFileDir$

###### iosApp-alpha配置 ######
Name: GRADLE_ENV_ALPHA
Group: GRADLE_ENV
Description: GRADLE_ENV_ALPHA
Program: /bin/bash
Arguments: -c "sh $ProjectFileDir$/iosApp/env/env_config_alpha.sh"
Working directory: $ProjectFileDir$

###### iosApp-release配置 ######
Name: GRADLE_ENV_RELEASE
Group: GRADLE_ENV
Description: GRADLE_ENV_RELEASE
Program: /bin/bash
Arguments: -c "sh $ProjectFileDir$/iosApp/env/env_config_release.sh"
Working directory: $ProjectFileDir$


<<<<<<<<<<<<<<<<<<<<< Xcode配置 >>>>>>>>>>>>>>>>>>>>
Edit Scheme -> Build/Pre-actions -> Run Script
其中shell输入框，默认值bin/sh不修改，Provide build settings from 必须选中iosApp

###### iosApp-debug ######

echo "VERSION_STATUS_DEVELOP" > "${PROJECT_DIR}/env/env_config.properties"
exit 0

###### iosApp-beta ######
echo "VERSION_STATUS_BETA" > "${PROJECT_DIR}/env/env_config.properties"
exit 0

###### iosApp-alpha ######
echo "VERSION_STATUS_ALPHA" > "${PROJECT_DIR}/env/env_config.properties"
exit 0

###### iosApp-release ######
echo "VERSION_STATUS_RELEASE" > "${PROJECT_DIR}/env/env_config.properties"
exit 0




