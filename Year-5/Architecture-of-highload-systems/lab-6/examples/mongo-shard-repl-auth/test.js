// run on router
use admin

sh.enableSharding("app")

use app
db.createCollection("data")
db.data.createIndex({ userId: 1 })

sh.shardCollection("app.data", { userId: 1 })

db.data.insertMany([
  { userId: 1, name: "Alice", age: 25 },
  { userId: 2, name: "Bob", age: 30 },
  { userId: 3, name: "Charlie", age: 35 },
  { userId: 4, name: "Diana", age: 28 },
  { userId: 5, name: "Eve", age: 32 }
])

sh.status()

db.data.getShardDistribution()
