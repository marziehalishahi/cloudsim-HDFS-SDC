package org.cloudbus.cloudsim.hdfs;

import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.SimEntity;
import org.cloudbus.cloudsim.core.SimEvent;

import java.util.HashMap;
import java.util.Map;

import java.util.*;

public class NameNode extends SimEntity {

    // لیست client
    protected List<Integer> clientList;

    // هرclient را با Broker خود نگاشت می کند (هر clientی در این شبیه‌سازی با یک Broker مطابقت دارد)
    protected Map<Integer, Integer> mapClientToBroker;

    // NameNode هر
    // در مورد
    // DataNode (vm) می داند
    protected List<Integer> dataNodeList;

    //هر DataNode نیز به Datacenter که به آن تعلق دارد نگاشت می شود
    protected Map<Integer, Integer> mapDataNodeToDatacenter;

    //هرid DataNode (vm) را با لیست بلوک هایی که به عنوان نام فایل در آن موجود است نگاشت می کند
    protected Map<Integer, List<String>> mapDataNodeToBlocks;

    // هر ID DataNode (vm) را با id Rack مرتبط در Datacenter خودش نگاشت می کند
    protected Map<Integer, Integer> mapDataNodeToRackId;

    // هر id DataNode (vm) را با حداکثر ظرفیت ذخیره سازی نگاشت می کند
    protected Map<Integer, Integer> mapDataNodeToCapacity;

    // نmap هر DataNode را با درصد پر شدن آن
    protected Map<Integer, Double> mapDataNodeToUsage;

    protected Map<Integer, Double> mapDataNodeToTotalCapacity;

    protected Map<Integer, Double> mapDataNodeToUsedSpace;

    // نmap هر Rack را با درصد پر شدن آن
    protected Map<Integer, Double> mapRackToUsage;

    // تعداد پیش‌فرض کپی در هر بلوک
    protected int defaultReplicas;

    // اندازه پیش فرض یک بلوک
    // protected int defaultBlockSize;

    /**
     * Creates a new entity.
     *
     * @param name the name to be associated with the entity
     */
    /**
     * یک موجودیت جدید ایجاد می کند.
     *
     * @param       name node را که باید با موجودیت مرتبط شود نامگذاری کنید
     */
    public NameNode(String name, int defaultBlockSize, int defaultReplicas) {
        super(name);
        // Initialize DataNodeStorage instances for each data node

        setClientList(new ArrayList<Integer>());
        setMapClientToBroker(new HashMap<Integer, Integer>());

        setDataNodeList(new ArrayList<Integer>());
        setMapDataNodeToDatacenter(new HashMap<Integer, Integer>());
        setMapDataNodeToBlocks(new HashMap<Integer, List<String>>());
        setMapDataNodeToRackId(new HashMap<Integer, Integer>());
        setMapDataNodeToCapacity(new HashMap<Integer, Integer>());
        setMapDataNodeToUsage(new HashMap<Integer, Double>());
        setMapDataNodeToTotalCapacity(new HashMap<Integer, Double>());
        initializeDataNodeStorage();
        setMapDataNodeToUsedSpace(new HashMap<Integer, Double>());



        // setDefaultBlockSize(defaultBlockSize);
        setDefaultReplicas(defaultReplicas);


    }



    @Override
    public void startEntity() {
        Log.printConcatLine(getName(), " is starting...");
    }

    @Override
    public void processEvent(SimEvent ev) {
        switch (ev.getTag()) {
            // پاسخ ویژگی های منابع
            case CloudSimTags.HDFS_NAMENODE_ADD_CLIENT:
                processAddClient(ev);
                break;
            // درخواست ویژگی های منبع
            case CloudSimTags.HDFS_NAMENODE_ADD_DN:
                processAddDataNode(ev);
                break;
            // پاسخ ایجاد VM
            case CloudSimTags.HDFS_NAMENODE_WRITE_FILE:
                processWriteFile(ev);
                break;
//                کدی که خودم اضافه کردم
//            case CloudSimTags.HDFS_NAMENODE_RESOURCE_DN :
//                processDynamicProcessing (ev) ;
//                break;

            //اگر شبیه سازی به پایان برسد
            case CloudSimTags.END_OF_SIMULATION:
                shutdownEntity();
                break;
            // سایر تگ های ناشناخته با این روش پردازش می شوند
            default:
                processOtherEvent(ev);
                break;
        }
    }

    // افزودن یک client جدید به لیست clients فعلی
    // ev contiene: int del client vm id, int del brokerId
    protected void processAddClient(SimEvent ev){
    //اشاره به id فعلی client,broker
        int[] data = (int[]) ev.getData();
        int currentClientId = data[0];
        int currentBrokerId = data[1];

        Log.printLine(CloudSim.clock() + ": " + getName() + ": Received client VM of ID " + currentClientId + ", belonging to broker " + currentBrokerId);


        this.clientList.add(currentClientId);
        //for (Integer i : getClientList())
        //    Log.printLine("Lista di Clients in NameNode: " + i);
        // aggiunge alla mappa il client id e il corrispondente broker id, necessario per rispedire indietro gli eventi
        this.mapClientToBroker.put(currentClientId, currentBrokerId);
    }

    // افزودن یک DataNode جدید به لیست DataNode های فعلی
//    زمانی که رویدادی از نوع CloudSimTags.HDFS_NAMENODE_ADD_DN دریافت می‌شود، متد processAddDataNode فراخوانی می‌شود.
    protected void processAddDataNode(SimEvent ev){

        int[] data = (int[]) ev.getData();
        int currentDataNodeId = data[0];
//این متد بررسی می کند که آیا DataNode با شناسه داده شده قبلاً در dataNodeList است (لیستی که DataNode های ثبت شده را ردیابی می کند).
// اگر از قبل در لیست باشد، متد بدون انجام هیچ اقدام دیگری برمی گردد.
        if (this.dataNodeList.contains(currentDataNodeId)){
            return;
        }

        int currentDatacenterId = data[1];
        int currentRackid = data[2];
        int currentStorageCapacity = data[3];
//سپس این روش پیامی را ثبت می کند که نشان می دهد NameNode یک DataNode VM با شناسه مشخص شده در یک Datacenter خاص دریافت کرده است.
        Log.printLine(CloudSim.clock() + ": " + getName() + ": Received DataNode VM of ID " + currentDataNodeId + ", in Datacenter " + currentDatacenterId);

        // گDataNode را به لیست DataNodes اضافه می کنیم
        this.dataNodeList.add(currentDataNodeId);
        //for (Integer i : getDataNodeList())
        //    Log.printLine("Lista di DataNodes in NameNode: " + i);

        // گDataNode را به Datacenter نگاشت می کنیم
        this.mapDataNodeToDatacenter.put(currentDataNodeId, currentDatacenterId);
        //Log.printLine("=== TEST: la mappa di DNs e Datacenters " + getMapDataNodeToDatacenter());

        // گDatacenter را به Rack نگاشت می کنیم
        this.mapDataNodeToRackId.put(currentDataNodeId, currentRackid);

        // گDataNode را به حداکثر ظرفیتش نگاشت می کنیم
        this.mapDataNodeToCapacity.put(currentDataNodeId, currentStorageCapacity);
        this.mapDataNodeToTotalCapacity.put(currentDataNodeId, (double) currentStorageCapacity);

        //درصد استفاده از DataNode فعلی را تنظیم می کنیم که 0٪ است.
        this.mapDataNodeToUsage.put(currentDataNodeId, 0.0);
        this.mapDataNodeToUsedSpace.put(currentDataNodeId,0.0);

        // اگر رک از قبل موجود نیست، استفاده از آن را روی 0% تنظیم می‌کنم، اما دیگر این کار را انجام نمی‌دهم،
        // زیرا بهmap دیگری نیز نیاز دارم که هر rack را به یک datacenter نگاشت کند،
        // در غیر این صورت بدیهی است کهid rack با هم همپوشانی دارند، مهم نیست.
        // وقتی به این مقدار نیاز دارم آن را با دست پیدا کنم
    }

    // نوشتن یک فایل جدید (Block) در خوشه HDFS، NameNode تصمیم می‌گیرد
    // که فایل و کپی‌های آن در کدام ماشین مجازی مقصد قرار است به l'evento ev è un array che contiene بروند.
    // : String nome del file, String: preferred number of replicas, String: blocksize
    protected void processWriteFile(SimEvent ev){
       //استخراج داده از eventهای دریافتی
        List<String> data = (List<String>) ev.getData();
         //استخراج اطلاعات مرتبط از دیتاها
        String fileName = data.get(0);
        int replicasNumber = Integer.parseInt(data.get(1));
        int blockSize = Integer.parseInt(data.get(2));  // اندازه بلوک در مگابایت
        int clientBrokerId = Integer.parseInt(data.get(3));  // شناسه VM مشتری که درخواست را ارسال می کند
         //اطلاعات مربوط به درخواست نوشتن دریافت شده را ثبت کنید
        Log.printLine(CloudSim.clock() + ": NameNode: received a write request, file name: " + fileName + ", replicas: " + replicasNumber + ", block size: " + blockSize + ", from client: " + clientBrokerId);

        // اگر تعداد replica ها تعریف نشده است، از مقدار replicas پیش فرض NameNode استفاده کنید
        if (replicasNumber == 0) {
            replicasNumber = defaultReplicas;
        }

        // فهرستی برای ذخیره شناسه های مقصد برای کپی ها
        List<Integer> destinationIds = new ArrayList<Integer>();
        //فهرست DataNode هایی که مقاصد قابل قبول هستند
        List<Integer> acceptableDestinations = new ArrayList<Integer>();

        // DATANODE
        // خالی است و اجازه ندارد پر باشد
        // ما بلوک اول را با درصد پر کردن کمتر در DATANODE می نویسیم
        // و نسخه های آن در رک های دیگر
        //  با حذف تمام DataNode که قبلاً حاوی بلوک هستند شروع می کنیم

            //بررسی کنید کدام DataNode برای ذخیره سازی بلوک مناسب است
        for (Integer iterDataNode : getDataNodeList()){
            // اگر DataNode حاوی بلوک نیست، آن را به عنوان یک مقصد قابل قبول در نظر بگیرید
            if (!getMapDataNodeToBlocks().containsKey(iterDataNode)) {
                // به این معنی است که DataNod خالی است
                acceptableDestinations.add(iterDataNode);
            } else if (!getMapDataNodeToBlocks().get(iterDataNode).contains(fileName)){
                acceptableDestinations.add(iterDataNode);
            }
        }
            //اگر مقصد مناسبی پیدا نشد، پیامی را وارد کرده و برگردانید
        if (acceptableDestinations.isEmpty()){
            Log.print(CloudSim.clock() + ": No suitable nodes were found to write the block to!");
            return;
        }

        // ما گره را از میان آن دسته از کاندیدهایی می گیریم
        // که درصد استفاده در آنها حداقل است (این HDFS درست نیست) بنابراین مقصد اولین نسخه را انتخاب می کنیم.
       //        DataNode را با حداقل درصد استفاده به عنوان مقصد اولین نسخه انتخاب کنید
        double minUsage = 999.9;
        Integer firstNode = null;

        for (Integer tempNode : acceptableDestinations){
            if (getMapDataNodeToUsage().get(tempNode) < minUsage){
                firstNode = tempNode;
                minUsage = getMapDataNodeToUsage().get(tempNode);
            }
        }

        // گره انتخاب شده را به لیست نتایج اضافه می کنم و آن را از کاندید های مقصد حذف می کنم
//        اولین گره انتخاب شده را به لیست شناسه های مقصد اضافه کنید
        destinationIds.add(firstNode);
//        اولین گره انتخابی را از لیست مقاصد قابل قبول حذف کنید
        acceptableDestinations.remove(firstNode);
//        تعداد ماکت های باقی مانده را به روز کنید
        replicasNumber--;   //من باید بدانم چند نسخه برای نوشتن برای چرخه بعدی باقی مانده است

        // تمامrack ها به جز رک در مقصد اول برای شروع قابل قبول هستند
//        قفسه های مناسب برای ماکت های باقی مانده را تعیین کنید
        Set<Integer> acceptableRacks = new HashSet<Integer>(getMapDataNodeToRackId().values());
        acceptableRacks.remove(getMapDataNodeToRackId().get(firstNode));

        double currentMinRackUsage;

        // حداکثر 2 گره در هر رک، تا زمانی که نسخه ها تمام شوند
        // رک را با کمترین استفاده کلی و با حداقل دو گره که جزء گره های قابل قبول هستند انتخاب می کنیم.
//        ماکت های باقی مانده را بین قفسه ها توزیع کنید
        double cycles = replicasNumber / (double) 2;
        cycles = (int) Math.ceil(cycles);
        for (int i = 1; i <= cycles; i++){
//شمارنده گره های معتبر در هر رک
            int validNodesPerRack = 0;
            List<Integer> originalAcceptableRacks = new ArrayList<Integer>(acceptableRacks);

            // اول از همه رک هایی را که حداقل 2 گره قابل قبول ندارند را از AcceptableRacks حذف می کنم.
//            رک هایی که حداقل 2 گره قابل قبول ندارند را بردارید
            for (Integer rack : originalAcceptableRacks){
                for (Integer node : acceptableDestinations){
                    if (getMapDataNodeToRackId().get(node).equals(rack)){
                        validNodesPerRack++;
                    }
                }
                if (validNodesPerRack < 2){
                    acceptableRacks.remove(rack);
                }
            }

            // اکنون من به دنبال رک در میان موارد قابل قبول با نسبت مصرف کمتر هستم (این HDFS درست نیست)
//            یک قفسه با حداقل استفاده کلی برای ماکت های باقی مانده انتخاب کنید
            currentMinRackUsage = 999.9;
            Integer chosenRack = null;

            for (Integer rack : acceptableRacks){
                if (findRackOverallUsage(rack) < currentMinRackUsage){
                    chosenRack = rack;
                    currentMinRackUsage = findRackOverallUsage(rack);
                }
            }
//رک انتخابی را از لیست قفسه های قابل قبول حذف کنید
            acceptableRacks.remove(chosenRack);

            // در این رک من دو گره با کمترین نسبت استفاده را انتخاب می کنم

            Integer previousNode = null;
            Integer chosenNode;

            for (int k = 0; k < 2; k++){

                double minNodeUsage = 999.9;
                chosenNode = null;
//یک گره با حداقل نسبت استفاده در مقاصد قابل قبول انتخاب کنید
                for (Integer tempNode : acceptableDestinations){
                    if (getMapDataNodeToUsage().get(tempNode) < minNodeUsage && getMapDataNodeToRackId().get(tempNode).equals(chosenRack) && !tempNode.equals(previousNode)){
                        chosenNode = tempNode;
                        minNodeUsage = getMapDataNodeToUsage().get(tempNode);
                    }
                }

                // گره انتخاب شده را به لیست نتایج اضافه می کنم و آن را از نامزدهای مقصد حذف می کنم
//                گره انتخاب شده را به لیست شناسه های مقصد اضافه کنید
                if (chosenNode != null){
                    previousNode = chosenNode;
                    destinationIds.add(chosenNode);
                    acceptableDestinations.remove(chosenNode);
//تعداد ماکت های باقی مانده را به روز کنید
                    replicasNumber--;
                    if (replicasNumber == 0){
                        break;
                    }
                }
            }

        }

        // درصد استفاده از گره هایی که تنظیم کردیم را تغییر میدهیم
//        درصد استفاده از گره هایی را که برای کپی انتخاب شده اند به روز کنید
        updateNodeUsage(destinationIds, blockSize);

        // ما بلوک را در DataNodes مربوطه در هش مپ که آن را اختصاص داده ایم اضافه می کنیم
        for (Integer i : destinationIds){
            if (getMapDataNodeToBlocks().get(i) == null)
                getMapDataNodeToBlocks().put(i, new ArrayList<String>(Collections.singleton(fileName)));
            else
                getMapDataNodeToBlocks().get(i).add(fileName);
        }
         // لیستی از VMها را به broker که آن را درخواست کرده است، ارسال می کنیم،
        // سپسbroker آن را در destVm Cloudlet درج می کند (destVM باید دوباره به عنوان یک لیست پیاده سازی شود)
        sendNow(clientBrokerId, CloudSimTags.HDFS_NAMENODE_RETURN_DN_LIST, destinationIds);

         for (Integer dataNode : mapDataNodeToTotalCapacity.keySet()) {
            double totalCapacity = mapDataNodeToTotalCapacity.get(dataNode);

            // ابتدا بررسی می کنیم کهdataNode درmapDataNodeToUsedSpace قرار دارد.
            if (mapDataNodeToUsedSpace.containsKey(dataNode)) {
                double usedSpace = mapDataNodeToUsedSpace.get(dataNode);

                // سپس utiltzation را محاسبه میکنیم
                double usageStatusPercentage = (usedSpace / totalCapacity) * 100;

                usageStatusPercentage=50;

                // لول ها را بر اساس درصد utilization اعمال میکنیم
                if (usageStatusPercentage >= 70 && usageStatusPercentage <= 100) {
                    // در لول اول 100 بیت دیتا به دیتای اصلی اضافه میکنیم
                    applyLevelOne(dataNode, 100);
                } else if (usageStatusPercentage >= 40 && usageStatusPercentage < 70) {
                    // در لول دوم 200 بیت دیتا به دیتای اصلی اضافه میکنیم
                    applyLevelTwo(dataNode, 200);
                } else if (usageStatusPercentage >= 10 && usageStatusPercentage < 40) {
                    // در لول سوم 400 بیت دیتا به دیتای اصلی اضافه میکنیم
                    applyLevelThree(dataNode, 400);
                }

                // Print or use the usage status percentage as needed
                System.out.println("Data Node " + dataNode + " Usage Status: " + usageStatusPercentage + "%");
            } else {
                System.out.println("Data Node " + dataNode + " information not available.");
            }
        }
    }

    // نmap برای ذخیره نمونه های DataNodeStorage مرتبط با هر datanode
    private Map<Integer, DataNodeStorage> dataNodeStorageMap = new HashMap<>();

    // متدی برای مقداردهی اولیه نمونه های DataNodeStorage برای هر datanode
    private void initializeDataNodeStorage() {
        for (Integer dataNode : mapDataNodeToTotalCapacity.keySet()) {
            dataNodeStorageMap.put(dataNode, new DataNodeStorage());
        }
    }

//    // متد هایی را برای اعمال بیت های اضافی بر اساس لول ها پیاده سازی می کنیم.
    private void applyLevelOne(int dataNode, int additionalBits) {
        if (dataNodeStorageMap.containsKey(dataNode)) {
            dataNodeStorageMap.get(dataNode).applyLevelOne(additionalBits);
            System.out.println("Level 1 applied to Data Node " + dataNode + ": " + additionalBits + " bits added");
        } else {
            System.out.println("Data Node " + dataNode + " information not available.");
        }
    }

    private void applyLevelTwo(int dataNode, int additionalBits) {
        if (dataNodeStorageMap.containsKey(dataNode)) {
            dataNodeStorageMap.get(dataNode).applyLevelTwo(additionalBits);
            System.out.println("Level 2 applied to Data Node " + dataNode + ": " + additionalBits + " bits added");
        } else {
            System.out.println("Data Node " + dataNode + " information not available.");
        }
    }

    private void applyLevelThree(int dataNode, int additionalBits) {
        if (dataNodeStorageMap.containsKey(dataNode)) {
            dataNodeStorageMap.get(dataNode).applyLevelThree(additionalBits);
            System.out.println("Level 3 applied to Data Node " + dataNode + ": " + additionalBits + " bits added");
        } else {
            System.out.println("Data Node " + dataNode + " information not available.");
        }
    }




//    private void applyLevelOne(int dataNode) {
//        // Assuming dataNodeStorageMap is a Map<Integer, DataNodeStorage> to store instances for each data node
//
//        // Check if the DataNodeStorage instance exists for the given data node
//        if (!dataNodeStorageMap.containsKey(dataNode)) {
//            // If not, create a new instance and put it in the map
//            dataNodeStorageMap.put(dataNode, new DataNodeStorage());
//        }
//
//        // Use the applyLevelOne method of DataNodeStorage
//        dataNodeStorageMap.get(dataNode).applyLevelOne(dataNode);
//    }

//    private void applyLevelTwo(int dataNode) {
//        // Assuming dataNodeStorageMap is a Map<Integer, DataNodeStorage> to store instances for each data node
//
//        // Check if the DataNodeStorage instance exists for the given data node
//        if (!dataNodeStorageMap.containsKey(dataNode)) {
//            // If not, create a new instance and put it in the map
//            dataNodeStorageMap.put(dataNode, new DataNodeStorage());
//        }
//
//        // Use the applyLevelOne method of DataNodeStorage
//        dataNodeStorageMap.get(dataNode).applyLevelTwo(dataNode);
//    }
//    private void applyLevelThree(int dataNode) {
//        // Assuming dataNodeStorageMap is a Map<Integer, DataNodeStorage> to store instances for each data node
//
//        // Check if the DataNodeStorage instance exists for the given data node
//        if (!dataNodeStorageMap.containsKey(dataNode)) {
//            // If not, create a new instance and put it in the map
//            dataNodeStorageMap.put(dataNode, new DataNodeStorage());
//        }
//
//        // Use the applyLevelOne method of DataNodeStorage
//        dataNodeStorageMap.get(dataNode).applyLevelThree(dataNode);
//    }


    protected double findRackOverallUsage(Integer rackId){
        double usage = 0.0;

        int totalCapacity = 0;
        double totalSpaceUsed = 0.0;

        for (Integer i : getMapDataNodeToRackId().keySet()){
            if (getMapDataNodeToRackId().get(i).equals(rackId)){
                totalCapacity += getMapDataNodeToCapacity().get(i);
                totalSpaceUsed += ( getMapDataNodeToUsage().get(i) * getMapDataNodeToCapacity().get(i));
            }
        }

        return (totalSpaceUsed / totalCapacity);
    }

    protected void updateNodeUsage (List<Integer> nodesToUpdate, int blockSize){

        double currentNodeUsage;
        double currentNodeCapacity;
        double currentNodeStorageAmount;
        double currentNodeUpdatedUsage;

        for (Integer currentNode : nodesToUpdate){
            currentNodeUsage = getMapDataNodeToUsage().get(currentNode);
            currentNodeCapacity = getMapDataNodeToCapacity().get(currentNode);
            currentNodeStorageAmount = (currentNodeUsage * currentNodeCapacity);

            currentNodeUpdatedUsage = (currentNodeStorageAmount + blockSize) / currentNodeCapacity;
            getMapDataNodeToUsage().put(currentNode, currentNodeUpdatedUsage);
        }
    }

    @Override
    public void shutdownEntity() {
        Log.printConcatLine(getName(), " is shutting down...");
    }

    /**
     * Process non-default received events that aren't processed by
     * the {@link #processEvent(org.cloudbus.cloudsim.core.SimEvent)} method.
     * This method should be overridden by subclasses in other to process
     * new defined events.
     *
     * @param ev a SimEvent object
     * @pre ev != null
     * @post $none
     * @todo to ensure the method will be overridden, it should be defined
     * as abstract in a super class from where new brokers have to be extended.
     */
    /**
     * رویدادهای دریافتی غیرپیش‌فرض را پردازش کنید که توسط پردازش نشده‌اند
     * روش {@link #processEvent(org.cloudbus.cloudsim.core.SimEvent)}.
     * این روش باید توسط زیر کلاس‌های دیگر برای پردازش لغو شود
     * رویدادهای جدید تعریف شده است.
     *
     * @param ev یک شی SimEvent
     * @pre ev != null
     * @post $ هیچ
     * @todo برای اطمینان از رد شدن روش، باید تعریف شود
     * به عنوان انتزاعی در یک کلاس فوق العاده که از آنجا brokers جدید باید گسترش یابند.
     */
    protected void processOtherEvent(SimEvent ev) {
        if (ev == null) {
            Log.printConcatLine(getName(), ".processOtherEvent(): ", "Error - an event is null.");
            return;
        }

        Log.printConcatLine(getName(), ".processOtherEvent(): Error - event unknown by this NameNode.");
    }

    // GETTERS AND SETTERS

    @SuppressWarnings("unchecked")
    public List<Integer> getClientList() {
        return clientList;
    }

    @SuppressWarnings("unchecked")
    public void setClientList(List<Integer> clientList) {
        this.clientList = clientList;
    }

    @SuppressWarnings("unchecked")
    public List<Integer> getDataNodeList() {
        return dataNodeList;
    }

    @SuppressWarnings("unchecked")
    public void setDataNodeList(List<Integer> dataNodeList) {
        this.dataNodeList = dataNodeList;
    }

    public Map<Integer, List<String>> getMapDataNodeToBlocks() {
        return mapDataNodeToBlocks;
    }

    public void setMapDataNodeToBlocks(Map<Integer, List<String>> mapDataNodeToBlocks) {
        this.mapDataNodeToBlocks = mapDataNodeToBlocks;
    }

    public int getDefaultReplicas() {
        return defaultReplicas;
    }

    public void setDefaultReplicas(int defaultReplicas) {
        this.defaultReplicas = defaultReplicas;
    }

    public Map<Integer, Integer> getMapClientToBroker() {
        return mapClientToBroker;
    }

    public void setMapClientToBroker(Map<Integer, Integer> mapClientToBroker) {
        this.mapClientToBroker = mapClientToBroker;
    }

    public Map<Integer, Integer> getMapDataNodeToDatacenter() {
        return mapDataNodeToDatacenter;
    }

    public void setMapDataNodeToDatacenter(Map<Integer, Integer> mapDataNodeToDatacenter) {
        this.mapDataNodeToDatacenter = mapDataNodeToDatacenter;
    }

    public Map<Integer, Integer> getMapDataNodeToRackId() {
        return mapDataNodeToRackId;
    }

    public void setMapDataNodeToRackId(Map<Integer, Integer> mapDataNodeToRackId) {
        this.mapDataNodeToRackId = mapDataNodeToRackId;
    }

    public Map<Integer, Integer> getMapDataNodeToCapacity() {
        return mapDataNodeToCapacity;
    }

    public void setMapDataNodeToCapacity(Map<Integer, Integer> mapDataNodeToCapacity) {
        this.mapDataNodeToCapacity = mapDataNodeToCapacity;
    }

    public Map<Integer, Double> getMapDataNodeToTotalCapacity() {
        return mapDataNodeToTotalCapacity;
    }

    public void setMapDataNodeToTotalCapacity (Map<Integer, Double> mapDataNodeToTotalCapacity) {
        this.mapDataNodeToTotalCapacity = mapDataNodeToTotalCapacity;
    }

    public Map<Integer, Double> getMapDataNodeToUsedSpace() {
        return mapDataNodeToUsedSpace;
    }
    public void setMapDataNodeToUsedSpace (Map<Integer, Double> mapDataNodeToUsedSpace) {
        this.mapDataNodeToUsedSpace = mapDataNodeToUsedSpace;
    }

    public Map<Integer, Double> getMapDataNodeToUsage() {
        return mapDataNodeToUsage;
    }

    public void setMapDataNodeToUsage(Map<Integer, Double> mapDataNodeToUsage) {
        this.mapDataNodeToUsage = mapDataNodeToUsage;
    }

    public Map<Integer, Double> getMapRackToUsage() {
        return mapRackToUsage;
    }

    public void setMapRackToUsage(Map<Integer, Double> mapRackToUsage) {
        this.mapRackToUsage = mapRackToUsage;
    }

    /*
    public int getDefaultBlockSize() {
        return defaultBlockSize;
    }

    public void setDefaultBlockSize(int defaultBlockSize) {
        this.defaultBlockSize = defaultBlockSize;
    }
     */
}
