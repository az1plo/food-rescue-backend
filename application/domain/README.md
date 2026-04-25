# Domain Module

Pure domain model for the Food Rescue Platform slice.

Current modeling choices:
- global system roles stay simple: `USER` and `ADMIN`
- a single `User` can manage zero or many `Business` locations
- business verification is modeled by `BusinessStatus`, not by changing user role
- future offer and reservation behavior hangs off an approved `Business`

Contains:
- entities and value objects for users, businesses, offers, reservations, and notifications
- repository ports for each aggregate root
- domain facade/service interfaces
- domain exceptions

Notes:
- `Business.ownerId` currently represents the responsible manager account for a location
- if the product later needs multiple staff members per location, extend this with a membership model instead of adding a global `BUSINESS` role

This module has no Spring, JPA, or REST dependencies.
