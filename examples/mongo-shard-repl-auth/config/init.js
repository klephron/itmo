rs.initiate()
rs.status()

use admin

db.createUser({user: 'admin', pwd: 'admin', roles: [{role: 'root', db: 'admin' }]})

rs.reconfig({
  _id: "rsc",
  configsvr: true,
  members: [
    {
      _id: 0,
      host: "mongodb-config:27017",
    },
  ]
})
