db = db.getSiblingDB("iot-db");

db.createUser({
    user: "zavar",
    pwd: "zavar",
    roles: [{ role: "readWrite", db: "iot-db" }],
});

print("MongoDB initialization script executed successfully.");
