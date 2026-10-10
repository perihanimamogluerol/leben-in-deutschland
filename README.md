# Leben in Deutschland

[![Android CI](https://github.com/perihanimamogluerol/leben-in-deutschland/actions/workflows/android-ci.yml/badge.svg)](https://github.com/perihanimamogluerol/leben-in-deutschland/actions/workflows/android-ci.yml)

A multilingual, offline-first study application for Germany's Leben in Deutschland and naturalization tests.

The project is being developed as a production-minded Android and iOS application. Its purpose is not only to deliver a useful learning product, but also to demonstrate architecture, testing, performance, accessibility, security, and cross-platform engineering decisions.

## Project Status

The project is currently in the foundation and architecture phase.

- Firebase content catalog: prepared
- Firestore release: uploaded and activated
- iOS project: created
- Android project: created
- Android CI: active
- Production user flows: pending
- Production automated test suite: pending
- iOS CI: pending

## Product Scope

- 460 German source questions from the official catalog
- 300 general questions
- 160 state-specific questions
- 21 supported languages
- German original content shown alongside translations
- Practice mode
- Wrong Answers study mode
- Timed mock examinations
- Anonymous Firebase authentication
- Offline-first content and progress
- Right-to-left support for Arabic, Persian/Dari, and Pashto

## Technology Direction

### Android

- Kotlin
- Jetpack Compose
- Coroutines and Flow
- Room
- DataStore
- Hilt
- WorkManager
- Firebase Authentication
- Cloud Firestore

### iOS

- Swift
- SwiftUI
- SwiftData
- Swift Concurrency
- Firebase Authentication
- Cloud Firestore

## Engineering Goals

- Keep the local database as the source of truth
- Support reliable offline usage after the first content download
- Publish content through immutable and versioned Firestore releases
- Keep business rules independent from the user interface
- Build testable and maintainable application layers
- Verify performance with measurable results
- Support accessibility and right-to-left layouts
- Document important architecture decisions
- Protect administrative credentials and user progress

## AI-Assisted Development

AI tools are used to accelerate research, translation, repetitive implementation, test-case discovery, documentation, and code review.

Architecture, security, product behavior, validation, testing strategy, performance decisions, and final code ownership remain the responsibility of the developer.

AI-generated translations will be clearly disclosed in the applications and may require human review.

## Repository Structure

```text
android/       Android application
ios/           iOS application
docs/          Product and engineering documentation
docs/adr/      Architecture Decision Records
firebase/      Firestore rules, indexes, and public configuration
scripts/       Content validation and administrative tooling
```

## Security and Content Notice

Administrative credentials, service-account files, private keys, and environment variables are never stored in this repository.

The German questions and answers are treated as the authoritative source content. Translations were generated with AI and may contain errors.