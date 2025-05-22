# Changelog

All notable changes to this project will be documented in this file.

## [Unreleased]

### Fixed
- Fixed build failure by updating Android Gradle Plugin from 7.4.2 to 8.2.2
- Updated Kotlin Gradle Plugin from 1.8.0 to 1.9.22
- Updated project structure to be compatible with Gradle 8.14
  - Added pluginManagement and dependencyResolutionManagement blocks to settings.gradle
  - Removed allprojects block from build.gradle
  - Updated clean task to use Gradle 8+ syntax

## [1.0] - Initial Release

- Initial application features