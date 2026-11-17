# Ateli Security Specification (Phase 0 TDD)

## 1. Data Invariants
1. **User Isolation**: Every document in `/users/{userId}`, `/users/{userId}/garments/{garmentId}`, and `/users/{userId}/outfits/{outfitId}` belongs exclusively to `userId` (`request.auth.uid == userId` and `data.userId == request.auth.uid`).
2. **Immutable Metadata**: `userId` and `createdAt` cannot be mutated on `update` (`incoming().userId == existing().userId && incoming().createdAt == existing().createdAt`).
3. **Strict Key Allowlisting**: Every `create` and `update` operation enforces `.keys().hasAll(...)` and `.keys().hasOnly(...)` / `.diff(existing()).affectedKeys().hasOnly(...)` to block shadow/ghost fields.
4. **Temporal & Numeric Bounds**: Timestamps (`createdAt`, `updatedAt`) must be Firestore `timestamp` and `<= request.time`. `purchasePrice` and `wearCount` must be non-negative bounded integers.

## 2. The "Dirty Dozen" Payloads
1. Unauthenticated read/list on `/users/alice/garments` -> REJECT
2. Cross-user read by `bob` on `/users/alice/garments/g1` -> REJECT
3. Identity spoofing: `alice` writes `userId: "bob"` inside `/users/alice/garments/g1` -> REJECT
4. Ghost field injection: `alice` sends `isAdmin: true` on garment creation -> REJECT
5. Negative price injection: `purchasePrice: -50000` -> REJECT
6. Invalid category enum: `category: "HAT"` -> REJECT
7. Oversized string DoS: `name` of 500 characters -> REJECT
8. Unbounded array DoS: `tpoTags` with 15 items -> REJECT
9. Immutable field tampering: modifying `createdAt` on garment update -> REJECT
10. Future timestamp spoofing: `createdAt` set in the future (`> request.time`) -> REJECT
11. Invalid ID poisoning: document ID with special characters or `> 128` chars -> REJECT
12. Value poisoning on update: setting `wearCount` to a string `"ten"` during update -> REJECT

## 3. Red Team Audit Report
- **Shadow Update Test**: Passed (`hasOnly` and `affectedKeys().hasOnly()` block ghost fields).
- **Email Spoofing Test**: Passed (no reliance on unverified email claims; strict UID ownership).
- **PII Blanket Test**: Passed (`/users/{userId}` restricts `get`/`list` to `request.auth.uid == userId`).
- **Query Trust Test**: Passed (all subcollections under `/users/{userId}/...` enforce `request.auth.uid == userId && resource.data.userId == request.auth.uid`).
- **Value Poisoning Test**: Passed (`isValidUserProfile`, `isValidGarment`, and `isValidSavedOutfit` wrap all `create` and `update` blocks).
