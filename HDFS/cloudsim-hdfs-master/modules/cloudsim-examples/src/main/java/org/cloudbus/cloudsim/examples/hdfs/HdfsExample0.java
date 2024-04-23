/*
 * Title:        CloudSim Toolkit
 * Description:  CloudSim (Cloud Simulation) Toolkit for Modeling and Simulation
 *               of Clouds
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2009, The University of Melbourne, Australia
 */

package org.cloudbus.cloudsim.examples.hdfs;

import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.hdfs.*;

import java.util.*;

import static org.cloudbus.cloudsim.core.CloudSimTags.HDFS_CLIENT;
import static org.cloudbus.cloudsim.core.CloudSimTags.HDFS_DN;
import static org.cloudbus.cloudsim.examples.hdfs.utils.HdfsUtils.*;

public class HdfsExample0 {

	/** The cloudlet list. */
	private static List<Cloudlet> cloudletList;

	/** The vmlist. */
	private static List<HdfsVm> vmList;

	/** The datacenter list */
	private static List<HdfsDatacenter> datacenterList;

	/**
	 * Creates main() to run this example
	 */
	public static void main(String[] args) {

		Log.printLine("Starting HdfsExample0...");

		try {

			// First step: Initialize CloudSim
			int num_user = 4;   // number of cloud users
			Calendar calendar = Calendar.getInstance();
			boolean trace_flag = false;  // means trace events

			CloudSim.init(num_user, calendar, trace_flag);

			// Second step: create the datacenters

			// DATACENTER PARAMETERS

			// values for PEs
			int datacenterPeMips = 1000;		// mips (performance) of a single PE
			int datacenterPeCount = 1;			// تعداد PE در هر host

			// values for Hosts
			int datacenterHostCount = 9;		// تعداد hostها (در مجموع در datacenter)
			int datacenterHostRam = 2048;		// مقدار رم برای هر host
			int datacenterHostStorage = 100000;	// مقدار storage اختصاص داده شده به هر host
			int datacenterHostBw = 10000;		// مقدار پهنای باند اختصاص داده شده به هر host

			// values for Storage
			int datacenterDiskCount = 9;		// تعداد Hard Drives در datacenter
			int datacenterDiskSize = 100000;	//  بcapacity برای هر Hard Drive

			// values for Racks
			int datacenterHostsPerRack = 3;		// تعداد host ها در هر Rack از Datacenter
			int datacenterBaseRackId = 0;

			// ایجاد یک آرایه با تمام پارامترهای ذخیره شده
			int[] datacenterParameters = new int[]{datacenterPeMips, datacenterPeCount, datacenterHostCount, datacenterHostRam,
					datacenterHostStorage, datacenterHostBw, datacenterDiskCount, datacenterDiskSize, datacenterHostsPerRack, datacenterBaseRackId};

			int[] datacenterParametersClient = new int[]{datacenterPeMips, datacenterPeCount, datacenterHostCount-8, datacenterHostRam,
					datacenterHostStorage, datacenterHostBw, datacenterDiskCount-8, datacenterDiskSize, datacenterHostsPerRack, datacenterBaseRackId};

			// برای راحتی Datacenters را در یک لیست قرار دادیم، (به خصوص برای متد printStorageList)
			datacenterList =  new ArrayList<HdfsDatacenter>();

			// Client datacenter
			HdfsDatacenter datacenter0 = createDatacenter("Datacenter_0", datacenterParametersClient);
			// Data Nodes datacenter (شناسه کلودلت شروع باید متفاوت باشد)
			HdfsDatacenter datacenter1 = createDatacenterDataNodes("Datacenter_1", 0, datacenterParameters);


			// Third step: Create Brokers

			// CLIENT (Main) BROKER
			HdfsDatacenterBroker broker = createBroker();
			int brokerId = broker.getId();

			// یک REPLICATION BROKER برای هر datacenter برای datanode (هر کدام با یک شناسه پایه cloudlet جدید)
			HdfsReplicationBroker replicationBroker = createBroker(100);
			datacenter1.setReplicationBrokerId(replicationBroker.getId());
			broker.getReplicationBrokersId().add(replicationBroker.getId());

			// creo il NameNode
			int blockSize = 10000;
			int defaultReplicas = 3;
			NameNode nameNode = new NameNode("NameNode1", blockSize, defaultReplicas);
//			Map<Integer, Double> Space = new HashMap<>();
//			Space.put(1,80000.0);
//			Space.put(2,10000.0);
//			Space.put(3,50000.5);
//			Space.put(4,50000.6);
//			Space.put(5,7770.3);
//			Space.put(6,9999.0);
//			Space.put(8,50000.7);
//			Space.put(7,22200.6);
//			Space.put(9,5000.3);
//			nameNode.setMapDataNodeToUsedSpace(Space);

			broker.setNameNodeId(nameNode.getId());

			// Fourth step: Create VMs

			// VM PARAMETERS
			int vmCount = 10;		// تعداد vms ایجاد شده
			int vmMips = 250;		// عperformance یک vm
			int vmPesNumber = 1;	// تعداد PEs
			int vmRam = 2048;		// vm memory (MB)
			long vmBw = 1000;		// مbandwidthموجود برای یک VM
			long vmSize = 10000;	// image size (MB)
			String vmm = "Xen";		// name of the Vm manager
			String cloudletSchedulerType = "Time"; // either "Time" shared or "Space" shared

            // ایجاد vm
			vmList = createVmList(vmCount, brokerId, vmMips, vmPesNumber, vmRam, vmBw, vmSize, vmm, cloudletSchedulerType);

			// این قسمت  متد createVm List را ادغام میکند.
			vmList.get(0).setHdfsType(HDFS_CLIENT);
			for (int i = 1; i < vmList.size(); i++)
				vmList.get(i).setHdfsType(HDFS_DN);

			// لیست vm را به broker ارسال میکند.
			broker.submitVmList(vmList);

			// مData nodes vms را به replication broker ارسال میکند
			List<HdfsVm> dnList = new ArrayList<HdfsVm>();

			// فقط Data Nodes Vms به لیستی که به replication broker ارسال می شود، اضافه می شود
			for (HdfsVm iterVm : vmList)
				if (iterVm.getHdfsType() == HDFS_DN)
					dnList.add(iterVm);

			replicationBroker.submitVmList(dnList);


			// Fifth step: Create Cloudlets

			cloudletList = new ArrayList<Cloudlet>();

			// CLOUDLET PARAMETERS
			int id = 0;
			long length = 40000;
			long fileSize = 300;
			long outputSize = 300;
			int pesNumber = 1;
			UtilizationModel utilizationModel = new UtilizationModelFull();

			//  دو بلوک برای انتقال از vm1 به vm2 و از vm1 به vm3 می سازیم

			// HDFS BLOCKS PARAMETERS
			int blockCount = 3;		// تعداد بلوک‌هایی که در cloudletها نوشته شده است

			List<File> blockList = createBlockList(blockCount, blockSize);

			// ابتدا باید فایل ها را داخل درایورهای Datacenter 0 ذخیره کنیم، زیرا client آنها را از آنجا می خواند
			datacenter0.addFiles(blockList);	// فایل های موجود در لیست را به عنوان یک سری فایل جداگانه اضافه میکنیم

			// ما باید لیستی از رشته ها را برای فیلد "requiredFiles" در سازنده HdfsCloudlet ایجاد کنیم.
			List<String> blockList1 = new ArrayList<String>();
			blockList1.add(blockList.get(0).getName());

			List<String> blockList2 = new ArrayList<String>();
			blockList2.add(blockList.get(1).getName());

			List<String> blockList3 = new ArrayList<String>();
			blockList3.add(blockList.get(2).getName());

			// در نهایت می توانیم cloudlet ها را ایجاد کنیم
			HdfsCloudlet cloudlet1 = new HdfsCloudlet(id, length, pesNumber, fileSize, outputSize, utilizationModel,
					utilizationModel, utilizationModel, blockList1, blockSize);
			cloudlet1.setUserId(brokerId);

			id++;
			HdfsCloudlet cloudlet2 = new HdfsCloudlet(id, length, pesNumber, fileSize, outputSize, utilizationModel,
					utilizationModel, utilizationModel, blockList2, blockSize);
			cloudlet2.setUserId(brokerId);

			id++;
			HdfsCloudlet cloudlet3 = new HdfsCloudlet(id, length, pesNumber, fileSize, outputSize, utilizationModel,
					utilizationModel, utilizationModel, blockList3, blockSize);
			cloudlet3.setUserId(brokerId);

			// شناسه vm مقصد را برای cloudlet ها تنظیم کنید
			//// اینها vms هستند که بلوک های HDFS باید در آن نوشته شوند
			// TODO: ovviamente questo ora non dovrebbe più servire, se la vede il NameNode
			//cloudlet1.setDestVmIds(vmList.get(1).getId());	// vm #2
			//cloudlet2.setDestVmIds(vmList.get(2).getId());	// vm #3

			// هcloudletها را به لیست اضافه کنید
			cloudletList.add(cloudlet1);
			cloudletList.add(cloudlet2);
			cloudletList.add(cloudlet3);

			// لیست cloudlet را به broker ارسال میکنیم
			broker.submitCloudletList(cloudletList);

			// کcloudlet ها را به vms متصل میکنیم، در این مورد هر دو باید روی vm1 اجرا شوند که Client vm است که فایل ها را می خواند.
			broker.bindCloudletToVm(cloudlet1.getCloudletId(),vmList.get(0).getId());
			broker.bindCloudletToVm(cloudlet2.getCloudletId(),vmList.get(0).getId());


			/*
			// NETWORK TOPOLOGY

			//Sixth step: configure network
			//load the network topology file
			NetworkTopology.buildNetworkTopology("topology.brite");

			//maps CloudSim entities to BRITE entities

			int briteNode=0;
			NetworkTopology.mapNode(datacenter0.getId(),briteNode);

			// TODO: datacenter0 e broker devono trovarsi nello stesso nodo, ma assegnarli allo stesso nodo dà errore, quindi bisogna modificare la topology
			briteNode=1;
			NetworkTopology.mapNode(broker.getId(),briteNode);

			briteNode=3;
			NetworkTopology.mapNode(datacenter1.getId(),briteNode);
			*/


			// Eighth step: Starts the simulation
			CloudSim.startSimulation();

			// Final step: Print results when simulation is over
			List<Cloudlet> newList = broker.getCloudletReceivedList();

			CloudSim.stopSimulation();

        	printCloudletList(newList);

        	// printing the status of the Drives in the Datacenters
        	printStorageList(datacenterList);

			Log.printLine("HdfsExample0 finished!");
		}
		catch (Exception e) {
			e.printStackTrace();
			Log.printLine("The simulation has been terminated due to an unexpected error");
		}
	}


	/**
	 * Creates a Datacenter
	 * @param name name of the datacenter
	 * @param requiredValues an array of 8 integers, which represent, in order:
	 *                       mips performance for a PE, number of PEs,
	 *                       number of Hosts, host RAM, host allocated Storage, host Bandwidth,
	 *                       number of HDDs, size of each HDD
	 * @return the datacenter object
	 * @throws ParameterException
	 */
	private static HdfsDatacenter createDatacenter(String name, int[] requiredValues) throws ParameterException{

		//List<HdfsHost> hostList;
		//List<Pe> peList;

		// values for Pes
		int mips = requiredValues[0];
		int pesNum = requiredValues[1];

		// values for Hosts
		int hostNum = requiredValues[2];
		int hostRam = requiredValues[3];
		int hostStorageSize = requiredValues[4];
		int hostBw = requiredValues[5];

		// values for Storage
		int hddNumber = requiredValues[6]; 	//
		int hddSize = requiredValues[7];

		int hostsPerRack = requiredValues[8];
		int baseRackId = requiredValues[9];

		// این متد، باید لیستی از Host ها، با افزایش ID، هر کدام با لیست Pe خاص خود را برگرداند
		// (هر کدام باید یک نمونه متفاوت از Pe List داشته باشند)
		List<HdfsHost> hostList = createHostList(hostNum, hostsPerRack, baseRackId, hostRam, hostStorageSize, hostBw, pesNum, mips);

		// DatacenterCharacteristics
		String arch = "x86";			// system architecture
		String os = "Linux";          	// operating system
		String vmm = "Xen";				// virtual machine manager
		double time_zone = 10.0;        // time zone this resource located
		double cost = 3.0;              // the cost of using processing in this resource
		double costPerMem = 0.05;		// the cost of using memory in this resource
		double costPerStorage = 0.001;	// the cost of using storage in this resource
		double costPerBw = 0.0;			// the cost of using bw in this resource

		LinkedList<Storage> storageList = createStorageList(hostList, hddSize);

		DatacenterCharacteristics characteristics = new DatacenterCharacteristics(
				arch, os, vmm, hostList, time_zone, cost, costPerMem, costPerStorage, costPerBw);

		// شی Datacenter را ایجاد و return میکنیم
		HdfsDatacenter datacenter = null;
		try {
			datacenter = new HdfsDatacenter(name, characteristics, new VmAllocationPolicySimple(hostList), storageList, 0);
			datacenterList.add(datacenter);

		} catch (Exception e) {
			e.printStackTrace();
		}

		return datacenter;

	}

	private static HdfsDatacenter createDatacenterDataNodes(String name, int replicationBrokerId, int[] requiredValues) throws ParameterException{

		//List<HdfsHost> hostList;
		//List<Pe> peList;

		// values for Pes
		int mips = requiredValues[0];
		int pesNum = requiredValues[1];

		// values for Hosts
		int hostNum = requiredValues[2];
		int hostRam = requiredValues[3];
		int hostStorageSize = requiredValues[4];
		int hostBw = requiredValues[5];

		// values for Storage
		int hddNumber = requiredValues[6];	//
		int hddSize = requiredValues[7];

		int hostsPerRack = requiredValues[8];
		int baseRackId = requiredValues[9];

		// این متد، باید لیستی از Host ها، با افزایش ID، هر کدام با لیست Pe خاص خود را برگرداند
		//  (هر کدام باید یک نمونه متفاوت از Pe List داشته باشند)
		List<HdfsHost> hostList = createHostList(hostNum, hostsPerRack, baseRackId, hostRam, hostStorageSize, hostBw, pesNum, mips);

		// DatacenterCharacteristics
		String arch = "x86";			// system architecture
		String os = "Linux";          	// operating system
		String vmm = "Xen";				// virtual machine manager
		double time_zone = 10.0;        // time zone this resource located
		double cost = 3.0;              // the cost of using processing in this resource
		double costPerMem = 0.05;		// the cost of using memory in this resource
		double costPerStorage = 0.001;	// the cost of using storage in this resource
		double costPerBw = 0.0;			// the cost of using bw in this resource

		LinkedList<Storage> storageList = createStorageList(hostList, hddSize);

		DatacenterCharacteristics characteristics = new DatacenterCharacteristics(
				arch, os, vmm, hostList, time_zone, cost, costPerMem, costPerStorage, costPerBw);

		// create and return the Datacenter object
		HdfsDatacenter datacenter = null;
		try {
			datacenter = new HdfsDatacenter(name, replicationBrokerId, characteristics, new VmAllocationPolicySimple(hostList), storageList, 0);
			datacenterList.add(datacenter);

		} catch (Exception e) {
			e.printStackTrace();
		}

		return datacenter;

	}

	// کاربران VMS و کلودلت‌ها را مطابق با قوانین خاص سناریوی شبیه‌سازی شده ارسال میکنند.
	private static HdfsDatacenterBroker createBroker(){

		HdfsDatacenterBroker broker = null;
		try {
			broker = new HdfsDatacenterBroker("Broker");
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return broker;
	}

	private static HdfsReplicationBroker createBroker(int cloudletBaseId){

		HdfsReplicationBroker broker = null;
		try {
			broker = new HdfsReplicationBroker("ReplicationBroker", cloudletBaseId);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return broker;
	}

}
