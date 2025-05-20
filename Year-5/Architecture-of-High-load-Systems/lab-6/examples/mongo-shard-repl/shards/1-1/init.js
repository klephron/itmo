rs.initiate()
rs.reconfig({
  _id: "rsh-1",
  members: [
    {
      _id: 0,
      host: "mongodb-shard-1-1:27017",
    },
    {
      _id: 1,
      host: "mongodb-shard-1-2:27017",
    }
  ]
})
// rs.status()
