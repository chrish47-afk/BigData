// Databricks notebook source
// MAGIC %python
// MAGIC ''' HASHING in CASSANDRA
// MAGIC
// MAGIC ------------------
// MAGIC Classroom Notes
// MAGIC ------------------
// MAGIC •	Recall that Cassandra organizes the nodes of a cluster into a logical ring. As a key-value store, Cassandra can look up or store data, locating where it physically lives in the cluster, based on the hash of the data’s key.  
// MAGIC •	We compute the hash of a key by passing the key into a hash function. A hash function is any function that can map data objects of arbitrary size down into values of fixed-size. For example, we might maps strings, which in principle can be any finite length, down to 64-bit integers, which are 8 bytes in size. You can find a discussion of hashing and hash functions in any introductory data structures and algorithms textbook.
// MAGIC
// MAGIC ------------------
// MAGIC  Other Notes 
// MAGIC ------------------
// MAGIC 1. Introduction to Hashing in Cassandra
// MAGIC    - Apache Cassandra is a distributed NoSQL database designed for high availability.
// MAGIC    - It uses **consistent hashing** to distribute data across nodes in a cluster.
// MAGIC
// MAGIC 2. Partitioning in Cassandra
// MAGIC    - Cassandra partitions data using a **partition key**.
// MAGIC    - The partition key is hashed using the **Murmur3 hashing function**.
// MAGIC    - The hash value determines which **virtual node (vnode)** stores the data.
// MAGIC
// MAGIC 3. Murmur3 Hashing
// MAGIC    - Cassandra uses Murmur3 as its default hashing function.
// MAGIC    - Murmur3 is a non-cryptographic, fast hash function optimized for hash-based lookups.
// MAGIC
// MAGIC 4. How Hashing Works in Cassandra
// MAGIC    - Given a **partition key**, Cassandra applies Murmur3 hash:
// MAGIC        ```
// MAGIC        token = Murmur3(partition_key)
// MAGIC        ```
// MAGIC    - The hash value (token) is mapped to a node in the **token ring**.
// MAGIC    - The node responsible for that token stores the data.
// MAGIC
// MAGIC 5. Consistent Hashing & Token Ring
// MAGIC    - Cassandra nodes are arranged in a **token ring**.
// MAGIC    - Each node owns a range of hash values (tokens).
// MAGIC    - When a new node joins, it takes over part of the token space, ensuring balanced distribution.
// MAGIC    - If a node fails, adjacent nodes in the ring take over its tokens.
// MAGIC
// MAGIC 6. Replication & Hashing
// MAGIC    - Cassandra replicates data across multiple nodes using **Replication Factor (RF)**.
// MAGIC    - The replication strategy defines how replicas are placed:
// MAGIC        - **SimpleStrategy**: Replicas are placed on consecutive nodes in the ring.
// MAGIC        - **NetworkTopologyStrategy**: Replicas are placed across data centers.
// MAGIC
// MAGIC '''

// COMMAND ----------

import scala.util.hashing.MurmurHash3

// COMMAND ----------

val s = "The Ballad of Wendell Scott"
val h = MurmurHash3.stringHash(s)
//This should give me a hash vaue of -761502732

// COMMAND ----------

// Imagine you have a Cassandra cluster made of eight nodes, whose names, which we will take as the unique ID of each machine are: { node0, node1, …, node7 }. 

//•	In your notebook, create a collection of eight node names according to the format above, i.e. node0, node1,…,node7.
//•	It’s possible to create the collection of names with a concise one-liner.  Indeed, you might want to try to create the names in such a manner to practice writing succinct Scala code, rather than  brute forcing things by explicitly typing out all the node names by hand.

// Creating them manually:
val nodes = Seq("node0", "node1", "node2", "node3", "node4", "node5", "node6", "node7")
println(nodes)

val nodeslist = (0 to 7).map(i => s"node$i").toSeq //list vs. vector
println(nodeslist)


// COMMAND ----------

// Computing the MurmurHash values for each of the node names and sort the results in ascending order by their MurmurHash values.
val nodeHashes = nodeslist.map(node => (node, MurmurHash3.stringHash(node)))
                          .sortBy(_._2)
println(nodeHashes)

// COMMAND ----------

// MAGIC %python
// MAGIC '''
// MAGIC ------------------
// MAGIC Logical Ring Positioning
// MAGIC ------------------
// MAGIC --- Why is the Range [-2,147,483,648 to 2,147,483,647]?
// MAGIC The range of hash values comes from the fact that Cassandra (and many hashing systems) use signed 32-bit integers for their MurmurHash3 hashing function. Let's break it down:
// MAGIC
// MAGIC -- Why Does MurmurHash3 Output 32-bit Signed Integers?
// MAGIC Cassandra uses MurmurHash3, which is a fast, non-cryptographic hash function that outputs values as signed 32-bit integers by default.
// MAGIC
// MAGIC When hashing a partition key (MurmurHash3.stringHash("some_key")), the result is always within the signed 32-bit range.
// MAGIC
// MAGIC -- Why Do We Map These Values to a 360° Ring?
// MAGIC Cassandra and similar distributed systems use consistent hashing, which places nodes on a circular hash space.
// MAGIC
// MAGIC Since hash values fall in [-2,147,483,648 to 2,147,483,647], we normalize them into a 0-360° scale.
// MAGIC This helps us visualize their distribution and assign partition keys to nodes efficiently.
// MAGIC
// MAGIC '''

// COMMAND ----------

//Imagine a user tries to insert a record whose key is “Transylvanian Christmas”. 
//What is the hash value associated with this key? 
//Which node (give the node’s name and Token ID) should the insertion of “Transylvanian Christmas” be destined for4?
val t = "Transylvanian Christmas"
val w = MurmurHash3.stringHash(t)


// COMMAND ----------

// MAGIC %python
// MAGIC '''
// MAGIC ------------------
// MAGIC Replication
// MAGIC ------------------
// MAGIC
// MAGIC --- What is Replication in Cassandra?
// MAGIC Replication in Cassandra ensures data availability and fault tolerance by copying data across multiple nodes. The number of copies (replicas) is determined by the Replication Factor (RF).
// MAGIC
// MAGIC --- What is SimpleStrategy?
// MAGIC SimpleStrategy is the most basic replication strategy in Cassandra. It is used for single-data-center deployments and determines which nodes store the replicas based on the token ring.
// MAGIC > How It Works
// MAGIC The first replica is placed on the node responsible for the token (i.e., the node where the partition key hashes to).
// MAGIC Additional replicas are placed on the next nodes clockwise in the token ring.
// MAGIC > Important: SimpleStrategy does NOT consider multiple data centers. If you're using a multi-data-center setup, you should use NetworkTopologyStrategy instead.
// MAGIC > Use only for a single datacenter and one rack. SimpleStrategy places the first replica on a node determined by the partitioner. Additional replicas are placed on the next nodes clockwise in the ring without considering topology (rack or datacenter location).
// MAGIC
// MAGIC --- Two replication strategies are available:
// MAGIC > SimpleStrategy: Use only for a single datacenter and one rack. If you ever intend more than one datacenter, use the NetworkTopologyStrategy.
// MAGIC > NetworkTopologyStrategy: Highly recommended for most deployments because it is much easier to expand to multiple datacenters when required by future expansion.
// MAGIC
// MAGIC '''