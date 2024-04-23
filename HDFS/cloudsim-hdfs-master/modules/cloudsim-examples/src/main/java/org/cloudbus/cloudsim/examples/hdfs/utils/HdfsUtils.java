package org.cloudbus.cloudsim.examples.hdfs.utils;

import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.SimEntity;
import org.cloudbus.cloudsim.hdfs.*;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public final class HdfsUtils{

    // لیستی از VMS را برای ارسال به broker ایجاد می کند
    // NOTE: vmm is always "Xen"
    public static List<HdfsVm> createVmList(int count, int userId, int mips, int pesNumber, int ram, long bw, long size,
                                            String vmm, String cloudletSchedulerType){

        LinkedList<HdfsVm> list = new LinkedList<HdfsVm>();

        // array of VMs
        HdfsVm[] vm = new HdfsVm[count];

        // به این صورت عمل می کند: vm آرایه ای با اندازه "vms" است، در حلقه، این آرایه را با تعداد زیادی vm جدید پر می کنیم،
        // هر یک از این vm نیز به لیست "list" اضافه می شود، که در انتها، خارج از فهرست بازگردانده می شود.
        for(int i = 0; i < count; i++){

            CloudletScheduler cloudletScheduler =
                    (cloudletSchedulerType.equals("Time")) ? new CloudletSchedulerTimeShared() : new CloudletSchedulerSpaceShared();

            vm[i] = new HdfsVm(i, userId, mips, pesNumber, ram, bw, size, vmm, cloudletScheduler);

            //برای ایجاد یک VM با سیاست space shared مشترک برای کلودلت‌ها:
            //vm[i] = Vm(i, userId, mips, pesNumber, ram, bw, size, priority, vmm, new CloudletSchedulerSpaceShared());

            list.add(vm[i]);
        }

        return list;
    }

    // فهرستی از Cloudlet ها را برای ارسال به broker ایجاد کنید.
    public static List<HdfsCloudlet> createCloudletList(int userId, int count, long length, long fileSize, long outputSize,
                                                        int pesNumber, UtilizationModel utilizationModel, List<String> blockList, int blockSize){

        LinkedList<HdfsCloudlet> list = new LinkedList<HdfsCloudlet>();

        HdfsCloudlet[] cloudlet = new HdfsCloudlet[count];

        for(int i = 0; i < count; i++){
            cloudlet[i] = new HdfsCloudlet(i, length, pesNumber, fileSize, outputSize, utilizationModel,
                    utilizationModel, utilizationModel, blockList, blockSize);
            // setting the owner of these Cloudlets
            cloudlet[i].setUserId(userId);
            list.add(cloudlet[i]);
        }

        return list;
    }

    // لیستی از PE ها را برای هر میزبان جداگانه ایجاد می کند
    // TODO: for now it only uses PeProvisionerSimple (ma è l'unico che esiste in Cloudsim in ogni caso)
    public static List<Pe> createPeList(int num, int mips){

        List<Pe> peList = new ArrayList<Pe>();

        for (int i = 0; i < num; i++){
            peList.add(new Pe(i, new PeProvisionerSimple(mips)));
        }

        return peList;
    }

    public static List<File> createBlockList(int count, int blockSize) throws ParameterException {

        List<File> blockList = new ArrayList<File>();

        String blockName;
        File block;

        for (int i = 0; i < count; i++){

            blockName = "Block_" + String.valueOf(i);
            block = new File(blockName, blockSize);
            blockList.add(block);
        }

        return blockList;
    }

    public static List<File> createBlockList(int count, int blockSize, int baseIndex) throws ParameterException {

        List<File> blockList = new ArrayList<File>();

        String blockName;
        File block;

        for (int i = 0; i < count; i++){

            blockName = "Block_" + String.valueOf(i+baseIndex);
            block = new File(blockName, blockSize);
            blockList.add(block);
        }

        return blockList;
    }

    // این linked list سپس linked list ذخیره سازی در DatacenterCharacteristics خواهد بود
    //  برای هرhost یک هارد دیسک ایجاد می کنم
    public static LinkedList<Storage> createStorageList(List<HdfsHost> hostList, int storageSize) throws ParameterException {

        LinkedList<Storage> storageList = new LinkedList<Storage>();

        for (int i = 0; i < hostList.size(); i++){
            String name = "HDD_Host" + String.valueOf(hostList.get(i).getId());
            HarddriveStorage tempStorage = new HarddriveStorage(name, storageSize, hostList.get(i).getId());
            hostList.get(i).setProperStorage(tempStorage);
            storageList.add(tempStorage);
        }

        return storageList;
    }

    // لیستی از host ها را در datacenter ایجاد می کند
    // TODO: Per ora il Vm scheduler è solo Time Shared e gli altri provisioners sono solo le versioni "Simple"
    public static List<HdfsHost> createHostList(int num, int hostsPerRack, int baseRackId, int ram, int storageSize, int bw, int pesNum, int mips){

        //  هر host جداگانه باید نمونه PeList خود را ایجاد کند
        List<HdfsHost> hostList = new ArrayList<HdfsHost>();

        for (int i = 0; i < num; i++){

            List<Pe> peList = createPeList(pesNum, mips);

            hostList.add(new HdfsHost(
                    i,
                    new RamProvisionerSimple(ram),
                    new BwProvisionerSimple(bw),
                    storageSize,
                    peList,
                    new VmSchedulerTimeShared(peList)));
        }

        // HOSTS PER RACK

        int rackId = baseRackId;
        int rackCounter = 0;

        for (int i = 0; i < num; i++){

            hostList.get(i).setRackId(rackId);
            rackCounter++;

            if (rackCounter == hostsPerRack){
                rackId++;
                rackCounter = 0;
            }
        }

        return hostList;
    }

    // PRINTING UTILITIES

    /**
     * Prints the Cloudlet objects
     * @param list  list of Cloudlets
     */
    public static void printCloudletList(List<Cloudlet> list) {
        int size = list.size();
        Cloudlet cloudlet;

        String indent = "    ";
        Log.printLine();
        Log.printLine("========== OUTPUT ==========");
        Log.printLine("Cloudlet ID" + indent + "STATUS" + indent +
                "Data center ID" + indent + "VM ID" + indent + "Time" + indent + "Start Time" + indent + "Finish Time");

        DecimalFormat dft = new DecimalFormat("###.##");
        for (int i = 0; i < size; i++) {
            cloudlet = list.get(i);
            Log.print(indent + cloudlet.getCloudletId() + indent + indent);

            if (cloudlet.getCloudletStatus() == Cloudlet.SUCCESS){
                Log.print("SUCCESS");

                Log.printLine( indent + indent + cloudlet.getResourceId() + indent + indent + indent + cloudlet.getVmId() +
                        indent + indent + dft.format(cloudlet.getActualCPUTime()) + indent + indent + dft.format(cloudlet.getExecStartTime())+
                        indent + indent + dft.format(cloudlet.getFinishTime()));
            }
        }
    }

    /**
     * Prints the Storage objects
     * @param list  Storage list from a single Datacenter
     */
    public static void printStorageList(List<HdfsDatacenter> list){

        String indent = "    ";
        Log.printLine();
        Log.printLine("========== STORAGE STATUS ==========");
        for (HdfsDatacenter datacenter : list){
            Log.printLine("Datacenter ID: " + (datacenter.getId()-1));

            for (Storage drive : datacenter.getStorageList()){
                HarddriveStorage tempDrive = (HarddriveStorage) drive;
                HdfsHost tempHost = (HdfsHost) datacenter.getHostList().get(tempDrive.getHostId());
                //Log.printLine("Rack ID : " + tempHost.getRackId());
                Log.printLine(indent + "Rack ID : " + tempHost.getRackId() + ", Drive: " + drive.getName() + ", Maximum capacity: " + drive.getCapacity() +
                        " MB, Used space: " + drive.getCurrentSize() + " MB, Free Space: " + drive.getAvailableSpace() + " MB, utilization: " + drive.getUtilization() +"%" + ", additionalBits: "  + drive.getAdditionalBits() + " MB " + ", File list:" );
                //Log.printLine(indent + indent + "File list: (Number of stored files: " + drive.getNumStoredFile() + ")");

                for (String fileName : drive.getFileNameList()){
                    Log.printLine(indent + indent + indent + "File: " + fileName);
                }
            }
        }
    }
}
