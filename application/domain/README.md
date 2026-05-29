# Domain Module

Pure domain model for the current Savr platform slice.

## Current modeling choices

- global system roles stay simple: `USER` and `ADMIN`
- a single `User` can manage zero or many `Business` locations
- business verification is modeled through `BusinessStatus`, not through a separate global business role
- public marketplace visibility is driven by active businesses and public offer states
- customer purchase flow is modeled by `Order`, `OrderPayment`, and `OrderPickupConfirmation`
- post-pickup feedback is modeled by `Review`
- support chat and offer assistant integrations are represented through provider-agnostic ports

## Contains

- entities and value objects for users, businesses, offers, orders, reviews, notifications, and support conversations
- repository ports for aggregate roots
- facade and service interfaces for domain use cases
- domain exceptions and validation rules

## Notes

- `Business.ownerId` currently represents the responsible manager account for a location
- the admin product slice is intentionally limited to pending business approvals
- if the product later needs multi-staff business management, extend ownership with a membership model instead of adding more global roles

This module has no Spring, JPA, or REST dependencies.

