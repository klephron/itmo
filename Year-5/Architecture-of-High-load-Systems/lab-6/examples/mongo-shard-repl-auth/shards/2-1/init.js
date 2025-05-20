rs.initiate() // initiate primary node

use admin
db.createUser({user: 'shard-admin', pwd: 'shard-admin', roles: [{role: 'root', db: 'admin' }]})
db.auth('shard-admin', 'shard-admin')
// db.runCommand({ connectionStatus: 1 })

rs.reconfig({
  _id: "rsh-2",
  members: [
    {
      _id: 0,
      host: "mongodb-shard-2-1:27017",
    },
    {
      _id: 1,
      host: "mongodb-shard-2-2:27017",
    }
  ]
})
// rs.status()
