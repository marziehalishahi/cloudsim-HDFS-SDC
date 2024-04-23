package org.cloudbus.cloudsim.hdfs;

import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.SimEvent;

import java.util.Iterator;
import java.util.List;

public class HdfsDatacenter extends Datacenter {

//    یک شمارنده برای تولید نام فایل های منحصر به فرد در datacenter را نشان می دهد.
    private int fileNameCounter;

    // وHDFS_CLIENT یا HDFS_DN که نقش vms داخل این Datacenter خواهد بود.
    protected int hdfsType;

    // وreplicationBroker برای Datacenter
    protected int replicationBrokerId;

    // ایجاد یک متغیر جدید با همان نام super، تا از طریق یک getter قابل دسترسی باشد
    private List<Storage> storageList;

    /**
     * یک شی Datacenter جدید را اختصاص می دهد. COSTRUTTORE PER I DATACENTERS DEI Clients.
     *
     * @param name               نامی که باید با این موجودیت مرتبط شود (طبق نیاز کلاس فوق)
     * @param characteristics    ویژگی های مرکز داده ای که باید ایجاد شود
     * @param vmAllocationPolicy خط مشی مورد استفاده برای تخصیص ماشین های مجازی به هاست
     * @param storageList        فهرستی از عناصر ذخیره سازی، برای شبیه سازی داده ها
     * @param schedulingInterval تأخیر زمان‌بندی برای پردازش هر رویداد دریافتی از مرکز داده
     * @throws Exception when one of the following scenarios occur:
     *                   <ul>
     *                     <li>creating this entity before initializing CloudSim package
     *                     <li>this entity name is <tt>null</tt> or empty
     *                     <li>this entity has <tt>zero</tt> number of PEs (Processing Elements). <br/>
     *                     No PEs mean the Cloudlets can't be processed. A CloudResource must contain
     *                     one or more Machines. A Machine must contain one or more PEs.
     *                   </ul>
     *                   هنگامی که یکی از سناریوهای زیر رخ می دهد:
     *       * <ul>
     *       * <li>ایجاد این موجودیت قبل از راه اندازی بسته CloudSim
     *       * <li>نام این نهاد <tt>null</tt> یا خالی است
     *       * <li>این موجودیت <tt>صفر</tt> تعداد PE (عناصر پردازش) دارد. <br/>
     *       * عدم وجود PE به این معنی است که Cloudlet ها قابل پردازش نیستند. یک CloudResource باید شامل باشد
     *       * یک یا چند ماشین. یک ماشین باید دارای یک یا چند PE باشد.
     *       * </ul>
     * @pre name != null
     * @pre resource != null
     * @post $none
     */

//    سconstructor برای ایجاد یکdatacenter  برای clientها.
    public HdfsDatacenter(String name, DatacenterCharacteristics characteristics, VmAllocationPolicy vmAllocationPolicy,
                          List<Storage> storageList, double schedulingInterval) throws Exception {
        super(name, characteristics, vmAllocationPolicy, storageList, schedulingInterval);

        fileNameCounter = 0;
        setHdfsType(CloudSimTags.HDFS_CLIENT);
    }
     // بconstructorبرای ایجاد یکdatacenter  برای datanodeها.
    //سconstructor برای Datacenter گره های داده، باید replicationBrokerId را بداند
    public HdfsDatacenter(String name, int replicationBrokerId, DatacenterCharacteristics characteristics, VmAllocationPolicy vmAllocationPolicy,
                          List<Storage> storageList, double schedulingInterval) throws Exception {
        super(name, characteristics, vmAllocationPolicy, storageList, schedulingInterval);

        fileNameCounter = 0;
        setReplicationBrokerId(replicationBrokerId);
        setHdfsType(CloudSimTags.HDFS_DN);

    }
    // این متد addFiles لیستی از فایل ها را به فضای ذخیره سازی دیتاسنتر اضافه می کند.
    // فایل های موجود در لیست را به عنوان یک سری فایل جداگانه اضافه می کند
    public void addFiles(List<File> fileList){

        for (File file : fileList) {
            super.addFile(file);
        }

    }

    // GETTERS AND SETTERS

    public int getHdfsType() {
        return hdfsType;
    }

    public void setHdfsType(int hdfsType) {
        this.hdfsType = hdfsType;
    }

    @Override
    public List<Storage> getStorageList() {
        return storageList;
    }

    @Override
    public void setStorageList(List<Storage> storageList) {
        this.storageList = storageList;
    }

    public int getReplicationBrokerId() {
        return replicationBrokerId;
    }

    public void setReplicationBrokerId(int replicationBrokerId) {
        this.replicationBrokerId = replicationBrokerId;
    }

    // NEW METHODS

    @Override
    public void processEvent(SimEvent ev) {
        int srcId = -1;

        switch (ev.getTag()) {
            // بررسی ویژگی های منابع
            case CloudSimTags.RESOURCE_CHARACTERISTICS:
                srcId = ((Integer) ev.getData()).intValue();
                sendNow(srcId, ev.getTag(), getCharacteristics());
                break;

            // جستجوی اطلاعات پویا منبع
            case CloudSimTags.RESOURCE_DYNAMICS:
                srcId = ((Integer) ev.getData()).intValue();
                sendNow(srcId, ev.getTag(), 0);
                break;

            case CloudSimTags.RESOURCE_NUM_PE:
                srcId = ((Integer) ev.getData()).intValue();
                int numPE = getCharacteristics().getNumberOfPes();
                sendNow(srcId, ev.getTag(), numPE);
                break;

            case CloudSimTags.RESOURCE_NUM_FREE_PE:
                srcId = ((Integer) ev.getData()).intValue();
                int freePesNumber = getCharacteristics().getNumberOfFreePes();
                sendNow(srcId, ev.getTag(), freePesNumber);
                break;

            // Cloudlet جدید از راه می رسد
            case CloudSimTags.CLOUDLET_SUBMIT:
                processCloudletSubmit(ev, false);
                break;

            // وCloudlet جدید می رسد، اما فرستنده درخواست تایید می کند
            case CloudSimTags.CLOUDLET_SUBMIT_ACK:
                processCloudletSubmit(ev, true);
                break;

            // وCloudlet قبلاً ارسال شده را لغو می کند
            case CloudSimTags.CLOUDLET_CANCEL:
                processCloudlet(ev, CloudSimTags.CLOUDLET_CANCEL);
                break;

            // یک Cloudlet قبلاً ارسال شده را متوقف می کند
            case CloudSimTags.CLOUDLET_PAUSE:
                processCloudlet(ev, CloudSimTags.CLOUDLET_PAUSE);
                break;

            // یک Cloudlet قبلاً ارسال شده را متوقف می کند
            // اما فرستنده درخواست تأیید می کند
            case CloudSimTags.CLOUDLET_PAUSE_ACK:
                processCloudlet(ev, CloudSimTags.CLOUDLET_PAUSE_ACK);
                break;

            // یک Cloudlet قبلا ارسال شده را از سر می گیرد
            case CloudSimTags.CLOUDLET_RESUME:
                processCloudlet(ev, CloudSimTags.CLOUDLET_RESUME);
                break;

            // یک Cloudlet که قبلاً ارسال شده است، اما فرستنده  درخواست را تایید می کند را از سر می گیرد

            case CloudSimTags.CLOUDLET_RESUME_ACK:
                processCloudlet(ev, CloudSimTags.CLOUDLET_RESUME_ACK);
                break;

            // یک Cloudlet قبلا ارسال شده را به منبع دیگری منتقل می کند
            case CloudSimTags.CLOUDLET_MOVE:
                processCloudletMove((int[]) ev.getData(), CloudSimTags.CLOUDLET_MOVE);
                break;

            // یک Cloudlet قبلا ارسال شده را به منبع دیگری منتقل می کند
            case CloudSimTags.CLOUDLET_MOVE_ACK:
                processCloudletMove((int[]) ev.getData(), CloudSimTags.CLOUDLET_MOVE_ACK);
                break;

            // وضعیت یک Cloudlet را بررسی می کند
            case CloudSimTags.CLOUDLET_STATUS:
                processCloudletStatus(ev);
                break;

            // بسته پینگ
            case CloudSimTags.INFOPKT_SUBMIT:
                processPingRequest(ev);
                break;

            case CloudSimTags.VM_CREATE:
                processVmCreate(ev, false);
                break;

            case CloudSimTags.VM_CREATE_ACK:
                processVmCreate(ev, true);
                break;

            case CloudSimTags.VM_DESTROY:
                processVmDestroy(ev, false);
                break;

            case CloudSimTags.VM_DESTROY_ACK:
                processVmDestroy(ev, true);
                break;

            case CloudSimTags.VM_MIGRATE:
                processVmMigrate(ev, false);
                break;

            case CloudSimTags.VM_MIGRATE_ACK:
                processVmMigrate(ev, true);
                break;

            case CloudSimTags.VM_DATA_ADD:
                processDataAdd(ev, false);
                break;

            case CloudSimTags.VM_DATA_ADD_ACK:
                processDataAdd(ev, true);
                break;

            case CloudSimTags.VM_DATA_DEL:
                processDataDelete(ev, false);
                break;

            case CloudSimTags.VM_DATA_DEL_ACK:
                processDataDelete(ev, true);
                break;

            case CloudSimTags.VM_DATACENTER_EVENT:
                updateCloudletProcessing();
                checkCloudletCompletion();
                break;

            /**
             *  HDFS TAGS
             */

            // ارسال cloudlet انتقال فایل del (Data cloudlet)
            case CloudSimTags.HDFS_CLIENT_CLOUDLET_SUBMIT:
                processClientCloudletSubmit(ev, false);
                break;

            // cloudlet انتقال فایل Ack del (Data cloudlet)
            case CloudSimTags.HDFS_CLIENT_CLOUDLET_SUBMIT_ACK:
                processClientCloudletSubmit(ev, true);
                break;

            // ارسالcloudlet انتقال فایل del (Data cloudlet)
            case CloudSimTags.HDFS_DN_CLOUDLET_SUBMIT:
                processDNCloudletSubmit(ev, false);
                break;

            // وcloudlet انتقال فایل Ack del (Data cloudlet)
            case CloudSimTags.HDFS_DN_CLOUDLET_SUBMIT_ACK:
                processDNCloudletSubmit(ev, true);
                break;

            // سایر تگ های ناشناخته با این روش پردازش می شوند
            default:
                processOtherEvent(ev);
                break;
        }
    }

    /**
     * Processes a Client Cloudlet submission, which reads a block from disk and sends it to the DN VM over the network
     *
     * @param ev information about the event just happened
     * @param ack indicates if the event's sender expects to receive
     * an acknowledge message when the event finishes to be processed
     *
     * @pre ev != null
     * @post $none
     */
    /**
     * یک ارسال Client Cloudlet را پردازش می کند، که یک بلوک را از دیسک می خواند و آن را از طریق شبکه به DN VM ارسال می کند.
     *
     * @param ev اطلاعات در مورد این رویداد به تازگی اتفاق افتاده است
     * @param ack نشان می دهد که آیا فرستنده رویداد انتظار دریافت دارد یا خیر
     * یک پیام تصدیق پس از پایان رویداد برای پردازش
     *
     * @pre ev != null
     * @post $ هیچ
     */

    // من دو پارامتر processCloudletMove را اضافه کردم
    protected void processClientCloudletSubmit(SimEvent ev, boolean ack) {

        // به روز رسانی در دیتاسنتر همه کلودولت ها
        // در همه هاست ها و تنظیم تاخیر در خود مرکز داده برای زمانی که امکان شروع عملیات بعدی وجود دارد.
        updateCloudletProcessing();

        try {
            // شی Cloudlet را دریافت می کند
            HdfsCloudlet cl = (HdfsCloudlet) ev.getData();

            // بررسی می کند که آیا کلودولت قبلاً تمام شده است یا خیر
            if (cl.isFinished()) {
                String name = CloudSim.getEntityName(cl.getUserId());
                Log.printConcatLine(getName(), ": Warning - Cloudlet #", cl.getCloudletId(), " owned by ", name,
                        " is already completed/finished.");
                Log.printLine("Therefore, it is not being executed again");
                Log.printLine();

                // توجه: اگر یک Cloudlet تمام شده باشد، پردازش نخواهد شد. بنابراین، اگر ack مورد نیاز باشد،
                // این روش یک نتیجه را ارسال می کند. اگر ack مورد نیاز نباشد، این روش نتیجه ای را ارسال نمی کند.
                if (ack) {
                    int[] data = new int[3];
                    data[0] = getId();
                    data[1] = cl.getCloudletId();
                    data[2] = CloudSimTags.FALSE;

                    // تگ منحصر به فرد = تگ عملیات
                    int tag = CloudSimTags.HDFS_CLIENT_CLOUDLET_SUBMIT_ACK;
                    sendNow(cl.getUserId(), tag, data);
                }

                // تغییر برچسب: برای اینکه broker بداند که کلودلتی که فایل را خوانده است بازگشته است،
                // اکنون می تواند CLOUDLET را که فایل را می نویسد به Data Node vm ارسال کند.
                sendNow(cl.getUserId(), CloudSimTags.CLOUDLET_RETURN, cl);

                return;
            }

            //  منابع این Datacenter خاص را در CLOUDLET تنظیم می کنیم
            cl.setResourceParameter(
                    getId(), getCharacteristics().getCostPerSecond(),
                    getCharacteristics().getCostPerBw());

            int userId = cl.getUserId();
            int vmId = cl.getVmId();

            // زمان لازم برای خواندن فایل های مورد نیاز از دیسک
            double fileTransferTime = predictFileTransferTime(cl.getRequiredFiles());

            send(cl.getUserId(), fileTransferTime, CloudSimTags.HDFS_CLIENT_CLOUDLET_RETURN, cl);

            // بیایید host را پیدا کنیم که در آن cloudlet vm قرار دارد
            Host host = getVmAllocationPolicy().getHost(vmId, userId);
            // وvm رو هم بگیر
            Vm vm = host.getVm(vmId, userId);
            CloudletScheduler scheduler = vm.getCloudletScheduler();
            // ما کلودلت را ارسال می کنیم و متد زمان پایان را برمی گرداند
            double estimatedFinishTime = scheduler.cloudletSubmit(cl, fileTransferTime);

            // اگر این کلودلت در صف اجرا باشد
            if (estimatedFinishTime > 0.0 && !Double.isInfinite(estimatedFinishTime)) {
                estimatedFinishTime += fileTransferTime;

                // Datacenter رویداد را برای خود ارسال می کند که باعث می شود تا زمان لازم منتظر بماند
                send(getId(), estimatedFinishTime, CloudSimTags.VM_DATACENTER_EVENT);
            }

            if (ack) {
                int[] data = new int[3];
                data[0] = getId();
                data[1] = cl.getCloudletId();
                data[2] = CloudSimTags.TRUE;

                // تگ منحصر به فرد = تگ عملیات
                int tag = CloudSimTags.CLOUDLET_SUBMIT_ACK;
                sendNow(cl.getUserId(), tag, data);
            }

        } catch (ClassCastException c) {
            Log.printLine(getName() + ".processClientCloudletSubmit(): " + "ClassCastException error.");
            c.printStackTrace();
        } catch (Exception e) {
            Log.printLine(getName() + ".processClientCloudletSubmit(): " + "Exception error.");
            e.printStackTrace();
        }


        // این روش روشی است که بازده های Cloudlet را ارسال می کند
        checkCloudletCompletion();
    }

    @Override
    protected double predictFileTransferTime(List<String> requiredFiles) {
        //return super.predictFileTransferTime(requiredFiles);

        double time = 0.0;

        Iterator<String> iter = requiredFiles.iterator();
        while (iter.hasNext()) {
            String fileName = iter.next();
            for (int i = 0; i < getStorageList().size(); i++) {
                Storage tempStorage = getStorageList().get(i);
                File tempFile = tempStorage.getFile(fileName);
                if (tempFile != null) {
                    time += tempFile.getTransactionTime();
                    break;
                }
            }
        }
        return time;
    }

    // متد ()predictFileTransferTime با روشی جایگزین می شود که فایل را روی دیسک می نویسد
    // و زمان تخمین زده شده برای انجام عملیات را برمی گرداند.
    protected void processDNCloudletSubmit(SimEvent ev, boolean ack) {

        // به روز رسانی در دیتاسنتر همه کلودولت ها در همه هاست ها
        // و تنظیم تاخیر در خود مرکز داده برای زمانی که امکان شروع عملیات بعدی وجود دارد.
        updateCloudletProcessing();

        try {
            // شی Cloudlet را دریافت می کند
            HdfsCloudlet cl = (HdfsCloudlet) ev.getData();

            // بررسی می کند که آیا این Cloudlet قبلاً تمام شده است یا خیر
            if (cl.isFinished()) {
                String name = CloudSim.getEntityName(cl.getUserId());
                Log.printConcatLine(getName(), ": Warning - Cloudlet #", cl.getCloudletId(), " owned by ", name,
                        " is already completed/finished.");
                Log.printLine("Therefore, it is not being executed again");
                Log.printLine();

                // توجه: اگر یک Cloudlet تمام شده باشد، پردازش نمی‌شود.
                // بنابراین، اگر ack مورد نیاز باشد، این روش نتیجه را برمی‌گرداند.
                // اگر ack مورد نیاز نباشد، این روش نتیجه ای را ارسال نمی کند.
                if (ack) {
                    int[] data = new int[3];
                    data[0] = getId();
                    data[1] = cl.getCloudletId();
                    data[2] = CloudSimTags.FALSE;

                    // تگ منحصر به فرد = تگ عملیات
                    int tag = CloudSimTags.HDFS_DN_CLOUDLET_SUBMIT_ACK;
                    sendNow(cl.getUserId(), tag, data);
                }

                //اگر Cloudlet قبلاً تمام شده باشد، به این معنی است که نوشتن برای فایل قبلاً انجام شده است،
                // بنابراین ما نباید  کار دیگری انجام دهیم،
                // تگ بازگشت یک CLOUDLET_RETURN ساده است،
                // زیرا ما نیازی به انجام کار دیگری نداریم. پس از آن
                sendNow(cl.getUserId(), CloudSimTags.CLOUDLET_RETURN, cl);

                return;
            }

            // ما منابع این Datacenter خاص را در cloudlet تنظیم می کنیم
            cl.setResourceParameter(
                    getId(), getCharacteristics().getCostPerSecond(),
                    getCharacteristics().getCostPerBw());

            int userId = cl.getUserId();
            int vmId = cl.getVmId();

            // زمان لازم برای خواندن فایل های مورد نیاز از دیسک
            double fileTransferTime = writeAndPredictTime(cl.getRequiredFiles().get(0), cl.getVmId(), cl.getBlockSize());

            //RePLICATION:
            // زمان لازم برای خواندن فایل های مورد نیاز از دیسک
            double fileReadTime = predictFileTransferTime(cl.getRequiredFiles());

            // پس از خواندن فایل برای کارگزار replication پیام ایجاد ماکت بعدی ارسال می شود
            send(replicationBrokerId, fileReadTime, CloudSimTags.HDFS_DN_CLOUDLET_RETURN, cl);



            // بیایید میزبانی را پیدا کنیم که در آن cloudlet vm قرار دارد
            Host host = getVmAllocationPolicy().getHost(vmId, userId);
            // vm رو هم بگیر
            Vm vm = host.getVm(vmId, userId);
            CloudletScheduler scheduler = vm.getCloudletScheduler();
            // ما کلودلت را ارسال می کنیم و متد زمان پایان را برمی گرداند
            double estimatedFinishTime = scheduler.cloudletSubmit(cl, fileTransferTime);

            // اگر این کلودلت در صف اجرا باشد
            if (estimatedFinishTime > 0.0 && !Double.isInfinite(estimatedFinishTime)) {
                estimatedFinishTime += fileTransferTime;

                //وDatacenter رویداد عمومی را برای خود ارسال می کند که باعث می شود تا زمان لازم منتظر بماند
                send(getId(), estimatedFinishTime, CloudSimTags.VM_DATACENTER_EVENT);
            }

            if (ack) {
                int[] data = new int[3];
                data[0] = getId();
                data[1] = cl.getCloudletId();
                data[2] = CloudSimTags.TRUE;

                // تگ منحصر به فرد = تگ عملیات
                int tag = CloudSimTags.HDFS_DN_CLOUDLET_SUBMIT_ACK;
                sendNow(cl.getUserId(), tag, data);
            }
        } catch (ClassCastException c) {
            Log.printLine(getName() + ".processDNCloudletSubmit(): " + "ClassCastException error.");
            c.printStackTrace();
        } catch (Exception e) {
            Log.printLine(getName() + ".processDNCloudletSubmit(): " + "Exception error.");
            e.printStackTrace();
        }

        // این روش روشی است که بازده های Cloudlet را ارسال می کند
        checkCloudletCompletion();
    }

    /**
     * Write the list of files and predict the total time necessary to perform the operation
     * Mi serve solo per un HDFS block, però per ora lascio la lista di files, penso userò un singolo file che fa da
     * blocco
     */
    /**
     * لیست فایل ها را بنویسید و کل زمان لازم برای انجام عملیات را پیش بینی کنید
     * من فقط برای یک بلوک HDFS به آن نیاز دارم، اما در حال حاضر لیست فایل ها را ترک می کنم، فکر می کنم از یک فایل استفاده کنم که به عنوان
     * مسدود کردن
     */
    protected double writeAndPredictTime(String fileName, int sourceVmId, int blockSize) {

        double time = 0.0;

        // افزایش نام فایل برای تکرار مفید است، زیرا متد ()addFile فایلی با همان نام در همان درایو اضافه نمی‌کند،
        // که دقیقاً همان چیزی است که ما می‌خواهیم (فقط مطمئن شوید که replica‌ها همان fileName اصلی هستند. )
        String theName = fileName;
        fileNameCounter++;

        // یک نمونه جدید از File ایجاد کنید
        File hdfsBlock = null;
        try {
            hdfsBlock = new File(theName, blockSize);
        } catch (ParameterException e) {
            Log.printLine(getName() + ".writeAndPredictTime(): " + "File creation error (invalid name or size).");
            e.printStackTrace();
        }

        // صاحب فایل را تنظیم کنید
        if (hdfsBlock != null) {
            // توجه: فراموش نکنید که این اکنون یک رشته است
            String ownerName = String.valueOf(sourceVmId);
            hdfsBlock.getFileAttribute().setOwnerName(ownerName);
        }

        // فایل "hdfsBlock" اکنون دارای یک نام فایل خاص، یک اندازه فایل و شناسه مالک vm (به عنوان یک رشته) است،
        // اکنون فایل را به فضای ذخیره سازی در داخل این Datacenter اضافه می کنیم
        // و زمان مورد نیاز را تخمین می زنیم (همه این کار انجام می شود. به طور خودکار با روش addFile())

        HarddriveStorage tempStorage = null;

        HdfsHost writingHost = null;
        for (Host tempHost : getHostList()){
            if (tempHost.getVmList().get(0).getId() == sourceVmId)
                writingHost = (HdfsHost) tempHost; // بیایید بفهمیم ماشین مجازی که می خواهد بلوک را بنویسد روی کدام میزبان است
        }

        //تمام درایوهای موجود در Datacenter را طی کنید
        for (int i = 0; i < getStorageList().size(); i++) {

            tempStorage = (HarddriveStorage) getStorageList().get(i);
            if (tempStorage.getHostId() == writingHost.getId()){
                // فایل را ذخیره کنید و زمان تخمینی را دریافت کنید
                time += tempStorage.addFile(hdfsBlock);
            }

            // اگر addFile به دلایلی ناموفق بود، زمان تنها برابر 0.0 است، بنابراین اگر addFile موفق بود، ما break
            // توجه: اگر فایلی با همین نام از قبل وجود داشته باشد، addFile از کار می افتد و 0.0 را برمی گرداند.
            if (time > 0.0){
                break;
            }
        }

        if (time == 0.0){
            Log.printLine(CloudSim.clock() + ": " + getName() + ": Couldn't add the file to any storage unit.");
        } else {
            Log.printLine(CloudSim.clock() + ": " + getName() + ": Successfully added file as " + hdfsBlock.getName()
            + " inside drive " + tempStorage.getName());
        }

        return time;
    }

    @Override
    protected void processVmCreate(SimEvent ev, boolean ack) {
        HdfsVm vm = (HdfsVm) ev.getData();

        boolean result;
        if (this.getHdfsType() == vm.getHdfsType()){
            result = getVmAllocationPolicy().allocateHostForVm(vm);
        } else {
            result = false;
        }


        if (ack) {
            int[] data = new int[3];
            data[0] = getId();
            data[1] = vm.getId();

            if (result) {
                data[2] = CloudSimTags.TRUE;
            } else {
                data[2] = CloudSimTags.FALSE;
            }
            send(vm.getUserId(), CloudSim.getMinTimeBetweenEvents(), CloudSimTags.VM_CREATE_ACK, data);
        }

        if (result) {
            getVmList().add(vm);

            if (vm.isBeingInstantiated()) {
                vm.setBeingInstantiated(false);
            }

            vm.updateVmProcessing(CloudSim.clock(), getVmAllocationPolicy().getHost(vm).getVmScheduler()
                    .getAllocatedMipsForVm(vm));
        }

    }

}
