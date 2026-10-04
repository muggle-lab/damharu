# Run only when changing Xcode project structure; normal builds need no Ruby gems.
require 'xcodeproj'
root = File.expand_path('../iosApp', __dir__)
project = Xcodeproj::Project.new(File.join(root, 'Damharu.xcodeproj'))
target = project.new_target(:application, 'Damharu', :ios, '16.0')
group = project.main_group.new_group('iosApp', 'iosApp')
target.source_build_phase.add_file_reference(group.new_file('DamharuApp.swift'))
group.new_file('Info.plist')
target.resources_build_phase.add_file_reference(group.new_file('Assets.xcassets'))
target.resources_build_phase.add_file_reference(group.new_file('LaunchScreen.storyboard'))
project.build_configurations.each do |config|
  config.build_settings['ENABLE_USER_SCRIPT_SANDBOXING'] = 'NO'
end
target.build_configurations.each do |config|
  s = config.build_settings
  s['PRODUCT_BUNDLE_IDENTIFIER'] = 'com.mugglelab.damharu'
  s['PRODUCT_NAME'] = 'Damharu'
  s['ASSETCATALOG_COMPILER_APPICON_NAME'] = 'AppIcon'
  s['INFOPLIST_FILE'] = 'iosApp/Info.plist'
  s['SWIFT_VERSION'] = '5.0'
  s['TARGETED_DEVICE_FAMILY'] = '1'
  s['CODE_SIGN_STYLE'] = 'Automatic'
  s['ENABLE_USER_SCRIPT_SANDBOXING'] = 'NO'
  s['FRAMEWORK_SEARCH_PATHS'] = ['$(inherited)', '$(SRCROOT)/../shared/build/xcode-frameworks/$(CONFIGURATION)/$(SDK_NAME)']
  s['OTHER_LDFLAGS'] = ['$(inherited)', '-framework', 'DamharuShared']
  s['LD_RUNPATH_SEARCH_PATHS'] = ['$(inherited)', '@executable_path/Frameworks']
  s['SWIFT_ACTIVE_COMPILATION_CONDITIONS'] = 'DEBUG $(inherited)' if config.name == 'Debug'
end
phase = target.new_shell_script_build_phase('Build Kotlin framework')
phase.shell_script = <<~SH
  set -eu
  # No OVERRIDE_KOTLIN_BUILD_IDE_SUPPORTED skip: this task is also what copies compose-resources
  # into the app bundle, and Android Studio does not do that for its own DerivedData.
  "$SRCROOT/../scripts/gradle.sh" :shared:embedAndSignAppleFrameworkForXcode
SH
phase.always_out_of_date = '1'
target.build_phases.delete(phase)
target.build_phases.unshift(phase)
tests = project.new_target(:ui_test_bundle, 'DamharuUITests', :ios, '16.0')
tests.add_dependency(target)
tests_group = project.main_group.new_group('iosAppUITests', 'iosAppUITests')
tests.source_build_phase.add_file_reference(tests_group.new_file('HomeSmokeTests.swift'))
tests.build_configurations.each do |config|
  config.build_settings['PRODUCT_BUNDLE_IDENTIFIER'] = 'com.mugglelab.damharu.uitests'
  config.build_settings['GENERATE_INFOPLIST_FILE'] = 'YES'
  config.build_settings['SWIFT_VERSION'] = '5.0'
  config.build_settings['TEST_TARGET_NAME'] = 'Damharu'
  config.build_settings['CODE_SIGN_STYLE'] = 'Automatic'
end
project.save
scheme = Xcodeproj::XCScheme.new
scheme.configure_with_targets(target, tests, launch_target: true)
scheme.save_as(project.path, 'Damharu', true)
puts project.path
