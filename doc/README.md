# SecureSMS - Architecture Documentation

This document contains PlantUML diagrams that illustrate the architecture and design of the SecureSMS Android application.

## Overview

SecureSMS is an Android application designed to provide secure SMS management with a clean, layered architecture following Android best practices and SOLID principles.

## Architecture Diagrams

### 1. Complete Architecture Diagram (`architecture.puml`)

This comprehensive class diagram shows:
- **UI Layer**: MainActivity with Jetpack Compose UI components
- **Service Layer**: Android bound service architecture with ISmsManagerService interface
- **Repository Layer**: Data access layer with reactive streams (StateFlow)
- **SMS Management Layer**: Direct interface to Android SMS system
- **Broadcast Receiver Layer**: Real-time SMS monitoring
- **Model Layer**: Data classes and enums
- **Utils Layer**: Permission management utilities

**Key Features**:
- Clean separation of concerns
- Dependency injection through constructor parameters
- Interface-based design for testability
- Reactive data streams using Kotlin StateFlow
- Proper Android service lifecycle management

### 2. Sequence Diagram (`sequence.puml`)

This diagram illustrates the complete data flow for major use cases:
- **Application Startup**: Permission handling and service binding
- **Initial Data Load**: Fetching SMS conversations from Android system
- **Data Refresh**: User-initiated or automatic data updates
- **Incoming SMS**: Real-time SMS reception and UI updates
- **View Conversation**: Displaying messages for a specific contact

**Key Patterns**:
- Asynchronous operations using Kotlin Coroutines
- Observer pattern for real-time updates
- Service-oriented architecture with proper lifecycle management

### 3. Component Diagram (`components.puml`)

This high-level view shows:
- **Layer Dependencies**: Clear separation between presentation, service, repository, and data layers
- **Android Integration**: How the app interfaces with Android SMS Provider and Broadcast system
- **Data Flow**: How SMS data flows from Android system to UI
- **Component Responsibilities**: Each component's role in the overall architecture

## Key Architectural Decisions

### 1. **Layered Architecture**
- **Presentation Layer**: UI components and user interaction
- **Service Layer**: Business logic and Android service management
- **Repository Layer**: Data access abstraction with caching
- **Data Layer**: Direct Android API integration

### 2. **Reactive Programming**
- Uses Kotlin StateFlow for reactive data streams
- Automatic UI updates when data changes
- Proper error handling and loading states

### 3. **Service-Oriented Design**
- Android bound service for background SMS operations
- Clean separation between UI and data operations
- Proper lifecycle management for long-running operations

### 4. **Permission Management**
- Centralized permission handling
- Graceful degradation when permissions are denied
- User-friendly permission request flow

### 5. **Real-time Updates**
- Broadcast receiver for incoming SMS messages
- Observer pattern for notifying UI components
- Automatic data synchronization

## Testing Strategy

The architecture supports comprehensive testing through:
- **Unit Tests**: Each layer can be tested independently
- **Integration Tests**: Service binding and data flow validation
- **UI Tests**: Compose UI testing with mock data
- **Mock Support**: Interface-based design enables easy mocking

## Dependencies

### Core Android
- Android SDK (API 34)
- Jetpack Compose for UI
- Android Services for background operations
- Content Providers for SMS access

### Kotlin
- Kotlin Coroutines for asynchronous operations
- StateFlow for reactive programming
- Data classes for immutable models

### Testing
- JUnit 4 for unit testing
- MockK for mocking
- Kotlin Coroutines Test for async testing
- Truth assertions for readable tests

## Security Considerations

1. **Permission Validation**: All SMS operations check for proper permissions
2. **Data Sanitization**: Phone numbers and messages are properly validated
3. **Background Processing**: Sensitive operations run in secure service context
4. **Error Handling**: Graceful handling of permission denials and system errors

## Performance Optimizations

1. **Lazy Loading**: SMS data loaded on demand
2. **Efficient Queries**: Optimized Android ContentProvider queries
3. **Background Processing**: Heavy operations run in background threads
4. **Memory Management**: Proper lifecycle management prevents memory leaks

## How to View Diagrams

1. **Online PlantUML Editor**: Copy the content of any `.puml` file to [PlantText](https://www.planttext.com/) or [PlantUML Online Server](http://www.plantuml.com/plantuml/uml/)

2. **VS Code Extension**: Install the "PlantUML" extension and preview the files directly

3. **IntelliJ/Android Studio**: Install PlantUML plugin for direct preview

4. **Command Line**: Use PlantUML jar to generate PNG/SVG files:
   ```bash
   java -jar plantuml.jar architecture.puml
   ```

## File Structure

```
doc/
├── architecture.puml    # Complete class diagram with all components
├── sequence.puml        # Data flow sequence diagram
├── components.puml      # High-level component architecture
└── README.md           # This documentation file
```

Each diagram serves a different purpose and audience:
- **Architecture**: For developers understanding class relationships
- **Sequence**: For understanding data flow and user interactions  
- **Components**: For high-level system overview and stakeholder communication
