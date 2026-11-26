const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

function validGarmentPayload(userId, now) {
  return {
    userId,
    name: "클래식 트렌치코트",
    brand: "무신사 스탠다드",
    category: "OUTER",
    subCategory: "트렌치코트",
    colorName: "베이지",
    colorHex: "#D8C3A5",
    season: "SPRING_FALL",
    pattern: "솔리드",
    thickness: "MEDIUM",
    waterResistant: true,
    tpoTags: ["출근", "데이트"],
    wearCount: 5,
    sourceType: "RECEIPT_AUTO",
    receiptStore: "무신사",
    createdAt: now,
    updatedAt: now,
  };
}

test("Unauthenticated user: cannot read garments or profile", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("garments").get()
  );
});

test("Authenticated user: can create and read own garment", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("garments")
      .doc("garment_1")
      .set(validGarmentPayload(ALICE_UID, now))
  );
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("garments")
      .where("userId", "==", ALICE_UID)
      .get()
  );
});

test("Authenticated user: cannot read or write another user's garment", async () => {
  const now = new Date();
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context
      .firestore()
      .collection("users")
      .doc(BOB_UID)
      .collection("garments")
      .doc("bob_g1")
      .set(validGarmentPayload(BOB_UID, now));
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb
      .collection("users")
      .doc(BOB_UID)
      .collection("garments")
      .doc("bob_g1")
      .get()
  );
  await assertFails(
    aliceDb
      .collection("users")
      .doc(BOB_UID)
      .collection("garments")
      .doc("bob_g2")
      .set(validGarmentPayload(BOB_UID, now))
  );
});

test("Shadow update / ghost field injection is rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();
  const badPayload = {
    ...validGarmentPayload(ALICE_UID, now),
    isVerifiedAdmin: true,
  };
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("garments")
      .doc("garment_bad")
      .set(badPayload)
  );
});

test("Negative wearCount or invalid category enum is rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("garments")
      .doc("garment_neg")
      .set({
        ...validGarmentPayload(ALICE_UID, now),
        wearCount: -1,
      })
  );
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("garments")
      .doc("garment_cat")
      .set({
        ...validGarmentPayload(ALICE_UID, now),
        category: "INVALID_CAT",
      })
  );
});

test("Authenticated user: can create and update user profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();
  const profileRef = aliceDb.collection("users").doc(ALICE_UID);
  await assertSucceeds(
    profileRef.set({
      userId: ALICE_UID,
      displayName: "김코디",
      heightCm: 168,
      weightKg: 56,
      shoulderWidthCm: 41,
      waistCm: 68,
      bodyShape: "HOURGLASS",
      skinToneHex: "#F3D6C1",
      preferredCity: "Seoul",
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    profileRef.update({
      heightCm: 170,
      updatedAt: new Date(),
    })
  );
});
