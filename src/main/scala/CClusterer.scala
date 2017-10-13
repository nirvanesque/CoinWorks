import java.io.{BufferedWriter, File, FileWriter}

import scala.collection.mutable
import scala.io.Source

/**
  * Created by cxa123230 on 10/7/2017.
  */
object CClusterer {
  val timePeriodMax: Int = 365

  def main(args: Array[String]): Unit = {
    val crdir: String = "D:\\Bitcoin\\createddata\\dailyOccMatrices\\"

    val startYear: Int = 2009
    val vectors = Array.ofDim[Long](400, (2017 - startYear + 1) * timePeriodMax)
    var currentTimePeriod = 0
    for (year: Int <- startYear to 2017 by 1) {
      for (timePeriod <- 1 to timePeriodMax by 1) {

        var updatedFileName: String = if (timePeriodMax == 52) timePeriod + "" else timePeriod + ""
        if (updatedFileName.length == 1) updatedFileName = "00" + updatedFileName
        else if (updatedFileName.length == 2) updatedFileName = "0" + updatedFileName

        val fileName: String = crdir + "occ" + year + "day" + updatedFileName + ".csv"
        val f: File = new File(fileName)

        if (f.exists) {
          val lines = Source.fromFile(fileName).getLines().toList
          var currentChainlet = 0;
          for (line <- lines) {
            val arr = line.split(",")
            for (c <- arr) {
              vectors(currentChainlet)(currentTimePeriod) = c.toLong
              //              println(currentChainlet+" "+currentTimePeriod+" "+c.toInt)
              currentChainlet += 1
            }
          }
        }
        else {
          //          println(fileName + " :((")
        }
        currentTimePeriod += 1
      }
    }



    val wr = new BufferedWriter(new FileWriter(crdir + "DailyVectors.txt")) //weeklyvectors
    for (key <- 0 to vectors.length - 1) {
      val clus = vectors(key)
      wr.append(clus.mkString("\t") + "\r\n")
    }
    wr.close()
    System.exit(1)
    val clusters = mutable.HashMap.empty[Int, ChainletCluster]
    for (chainletId <- 0 to vectors.length - 1) {
      clusters(chainletId) = new ChainletCluster(chainletId, vectors(chainletId))
    }


    var s = 0
    val sims = Array.ofDim[Double](vectors.length, vectors.length)
    while (clusters.size > 1) {
      s += 1
      println("step " + s)
      var maxSim = -1.0
      var merge1 = -1
      var merge2 = -1
      for (cc1 <- clusters.keySet) {
        for (cc2 <- clusters.keySet) {
          val c1 = clusters(cc1)
          val c2 = clusters(cc2)
          if (c1 != c2) {
            var avgSim = 0.0
            var i = 0
            if (sims(cc1)(cc2) == 0 || sims(cc1)(cc2) == -1) {
              //              println("computing "+cc1+" "+cc2)
              for (m1 <- c1.getMemberVectors) {
                for (m2 <- c2.getMemberVectors) {
                  avgSim += CosineSim.cosineSimilarity(m1, m2)
                  i += 1
                }
              }
              avgSim = avgSim / i
              sims(cc1)(cc2) = avgSim
            }
            avgSim = sims(cc1)(cc2)
            if (avgSim > maxSim) {
              maxSim = avgSim
              merge1 = cc1
              merge2 = cc2
              //              println(cc1 + " " + cc2 + " " + maxSim + " maxSim")
            }
          }
        }
      }

      if (maxSim < 0.7) {

        val wr = new BufferedWriter(new FileWriter(crdir + "DailyClusters.txt")) //weeklyvectors
        for (key <- clusters.keySet.toSeq.sorted) {
          val clus: ChainletCluster = clusters(key)


          val d = clus.getMemberIds()
          for (a <- d) {
            wr.append((1 + (a / 20)) + ":" + (1 + a % 20) + ",")
          }
          wr.append("\r\n")
        }
        wr.close()
        println(clusters.size + " clusters")




        for (clusteringMethod <- List("avg", "median", "max")) {
          val wr = new BufferedWriter(new FileWriter(crdir + clusteringMethod + "ClusteredDailyVectors.txt")) //weeklyvectors
          for (key <- clusters.keySet.toSeq.sorted) {
            val clus: ChainletCluster = clusters(key)
            val vec = clus.getClusterVector(clusteringMethod)
            wr.append(vec.mkString("\t"))
            wr.append("\r\n")
            println(vec.mkString(","))
          }
          wr.close()
        }
        System.exit(1)
      }
      val cluster1 = clusters(merge1)
      val cluster2 = clusters(merge2)
      for (k <- clusters.keySet) {
        sims(merge1)(k) = -1

      }
      for (chainletVector <- cluster2.getMemberVectors) {
        cluster1.add(chainletVector)
      }
      for (chainletId <- cluster2.getMemberIds()) {
        cluster1.addId(chainletId)
      }
      clusters(merge1) = cluster1
      println("(" + merge1 + "," + merge2 + "):" + maxSim + " remaining " + clusters.size)
      clusters.remove(merge2)

    }


  }

}