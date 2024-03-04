package org.cloudbus.cloudsim.hdfs;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.DatacenterBroker;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.SimEvent;
import org.cloudbus.cloudsim.lists.VmList;
import org.cloudbus.cloudsim.core.SimEntity;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class HdfsDatacenterBroker extends DatacenterBroker {

    protected int currentCloudletMaxId;

    protected int nameNodeId;

    protected HdfsCloudlet stagedCloudlet;

    protected List<Integer> replicationBrokersId;

    // used only to print prettier logs
    DecimalFormat df = new DecimalFormat("#.###");

    /**
     * Created a new DatacenterBroker object. Remember to set the name node as well after creation!
     *
     * @param name name to be associated with this entity (as required by {@link SimEntity} class)
     * @throws Exception the exception
     * @pre name != null
     * @post $none
     */
    /**
     * یک شی DatacenterBroker جدید ایجاد کرد. به یاد داشته باشید که پس از ایجاد، گره نام را نیز تنظیم کنید!
     *
     * نام نام @param برای مرتبط شدن با این نهاد (طبق نیاز کلاس {@link SimEntity})
     * @ throws استثنا استثنا
     * @pre name != null
     * @post $ هیچ
     */
    public HdfsDatacenterBroker(String name) throws Exception {
        super(name);
        currentCloudletMaxId = 0;
        setReplicationBrokersId(new ArrayList<Integer>());
    }

    // MANUFACTURER FOR RePLICATION BROKERS
    // ، حداکثر id
    // cloudlet
    // را روی یک عدد بالاتر میگذاریم، به عنوان مثال 100+
    public HdfsDatacenterBroker(String name, int cloudletStartId) throws Exception {
        super(name);
        currentCloudletMaxId = cloudletStartId;
    }

    @Override
    public void processEvent(SimEvent ev) {
        switch (ev.getTag()) {
            // درخواست ویژگی های منبع
            case CloudSimTags.RESOURCE_CHARACTERISTICS_REQUEST:
                processResourceCharacteristicsRequest(ev);
                break;
            // پاسخ ویژگی های منابع
            case CloudSimTags.RESOURCE_CHARACTERISTICS:
                processResourceCharacteristics(ev);
                break;
            // پاسخ ایجاد VM
            case CloudSimTags.VM_CREATE_ACK:
                processVmCreate(ev);
                break;
            //اcloudlet برگشت
            case CloudSimTags.CLOUDLET_RETURN:
                processCloudletReturn(ev);
                break;
            // اگر شبیه سازی به پایان برسد
            case CloudSimTags.END_OF_SIMULATION:
                shutdownEntity();
                break;

            /**
             *  HDFS tags
             */

            //اcloudlet برگشت
            case CloudSimTags.HDFS_CLIENT_CLOUDLET_RETURN:
                processClientCloudletReturn(ev);
                break;
            // یک Cloudlet DN تمام شده برمی گردد، این برای RePLICATION استفاده می شود
            case CloudSimTags.HDFS_DN_CLOUDLET_RETURN:
                processSendReplicaCloudlet(ev);
                break;
            // نnamenode لیستی از vms را برمی‌گرداند که در آن بلوک بنویسد
            case CloudSimTags.HDFS_NAMENODE_RETURN_DN_LIST:
                processSendDataCloudlet(ev);
                break;

            // سایر تگ های ناشناخته با این روش پردازش می شوند
            default:
                processOtherEvent(ev);
                break;
        }
    }

    // کCloudlet کلاینت که فایل را می خواند برمی گردد.حالا باید Cloudlet کلاینت را دوباره به DN ارسال کنیم.
    protected void processClientCloudletReturn(SimEvent ev) {

        HdfsCloudlet originalCloudlet = (HdfsCloudlet) ev.getData();

        Log.printConcatLine(df.format(CloudSim.clock()), ": ", getName(), ": Cloudlet ", originalCloudlet.getCloudletId(),
                ": the block has been read, communicating with the NameNode...");

            //اختصاص id جدید به کلودلت
        stagedCloudlet = HdfsCloudlet.cloneCloudletAssignNewId(originalCloudlet, currentCloudletMaxId + 1);

        // ه id  vm اصلی را ذخیره کنید تا بتونیم بلاک آن را در DN پیگیری کنیم
        stagedCloudlet.setSourceVmId(originalCloudlet.getVmId());

        //  تنظیم لیست vms های مقصد است که برای آن NameNode نیاز است
        List<String> nameNodeData = new ArrayList<String>();
        nameNodeData.add(originalCloudlet.getRequiredFiles().get(0));
        nameNodeData.add(Integer.toString(originalCloudlet.getReplicaNum()));
        nameNodeData.add(Integer.toString(originalCloudlet.getBlockSize()));
        nameNodeData.add(Integer.toString(getId()));
        sendNow(getNameNodeId(), CloudSimTags.HDFS_NAMENODE_WRITE_FILE, nameNodeData);

        // قطعه ای که اینجا بود اکنون وارد فرآیند Send Data Cloudlet() شده است.

    }

    // TODO: این روش
    //  ! namenode
    //  لیستی از vms را پیدا کرد که فایل باید در آن نوشته شود، بنابراین...
    protected void processSendDataCloudlet(SimEvent ev) {

        // این لیست را باز می کنم و لیست id های vms را دریافت می کنم

        List<Integer> destinationVms = (List<Integer>) ev.getData();

        // آن را از روش بالا کپی کنید (processClientCloudletReturn()) و
        // در واقع VM DN را به عنوان id VM جدید برای کلودت تنظیم کنید.

        stagedCloudlet.setVmId(destinationVms.get(0));

        // اکنون اولین vm را از لیست vms مقصد حذف می کنم و به destVmIds اضافه می کنم
        destinationVms.remove(0);
        stagedCloudlet.setDestVmIds(destinationVms);

        //همچنین می توانید از متد bind استفاده کنید که همان کار را انجام می دهد
        // bindCloudletToVm(cloudlet.getCloudletId(), cloudlet.getVmId());
        //کلودلت را به لیست ابرهای ارسالی اضافه کنید
        getCloudletList().add(stagedCloudlet);

        // نمی دانم ابتدا VM را تنظیم کنم و سپس به CloudletList اضافه کنم یا برعکس آن را انجام دهم، خواهیم دید...

        /* ri-eseguiamo questo metodo, che ora troverà il nuovo unbound cloudlet nella lista, e lo invierà
        alla VM appropriata, inoltre settando la posizione del broker uguale a quella del client nella topology,
        avremo una corretta simulazione del delay per l'invio del file tramite network
        */
        /* این متد را مجددا اجرا کنید که اکنون ابر ناپیوسته جدید را در لیست پیدا کرده و به VM مناسب ارسال کنید،
        همچنین با قرار دادن موقعیت بروکر برابر با مشتری در توپولوژی، یک موقعیت صحیح خواهیم داشت.
        شبیه سازی تاخیر برای ارسال فایل ها از طریق شبکه
         */
        submitDNCloudlets();

    }

    // برای پردازش Send DataCloudlet یکسان است
    // اما کل Hdfs Cloudlet را به عنوان یک رویداد دریافت می کند تا به مقصد بعدی تغییر مسیر دهد.
    protected void processSendReplicaCloudlet(SimEvent ev) {

        HdfsCloudlet originalCloudlet = (HdfsCloudlet) ev.getData();

         Log.printConcatLine(df.format(CloudSim.clock()), ": ", getName(), ": ReplicationCloudlet ", originalCloudlet.getCloudletId(),
                ": the block has been read, sending it to the Data Node...");


        stagedCloudlet = HdfsCloudlet.cloneCloudletAssignNewId(originalCloudlet, currentCloudletMaxId + 1);

        // شid  اصلیvm را ذخیره کنید تا بتوانیم بلاک آن را در DN پیگیری کنیم
        stagedCloudlet.setSourceVmId(originalCloudlet.getVmId());

        // لیست vms مقصد را دریافت کنید
        List<Integer> destinationVms = originalCloudlet.getDestVmIds();

        if (destinationVms.isEmpty()){
            Log.printLine(df.format(CloudSim.clock()) + ": " + getName() + ": The replication pipeline is over");
            return;
        }

        //آن را از روش بالا کپی کنید (processClientCloudletReturn()) و
        // در واقع VM DN را به عنوان شناسه VM جدید برای کلودت تنظیم کنید.

        stagedCloudlet.setVmId(destinationVms.get(0));

        // اکنون اولین vm را از لیست vms مقصد حذف می کنم و به destVmIds اضافه می کنم
        destinationVms.remove(0);
        stagedCloudlet.setDestVmIds(destinationVms);

        // همچنین می توانید از متد bind استفاده کنید که همان کار را انجام می ده
        // د bindCloudletToVm(cloudlet.getCloudletId(), cloudlet.getVmId());
        // کلودلت را به لیست ابرهای ارسالی اضافه کنید
        getCloudletList().add(stagedCloudlet);

        //نمی دانم ابتدا VM را تنظیم کنم و سپس به CloudletList اضافه کنم یا برعکس آن را انجام دهم، خواهیم دید...

        /* ri-eseguiamo questo metodo, che ora troverà il nuovo unbound cloudlet nella lista, e lo invierà
        alla VM appropriata, inoltre settando la posizione del broker uguale a quella del client nella topology,
        avremo una corretta simulazione del delay per l'invio del file tramite network
        */
        /* این متد را مجددا اجرا کنید که اکنون ابر ناپیوسته جدید را در لیست پیدا کرده و به VM مناسب ارسال کنید،
         همچنین با قرار دادن موقعیت بروکر برابر با مشتری در توپولوژی،
        یک موقعیت صحیح خواهیم داشت. شبیه سازی تاخیر برای ارسال فایل ها از طریق شبکه
         */
        submitDNCloudlets();

    }


    // باید (در صورتی که vm برای یک کلاینت یا توسط DN باشد) با NameNode VM ایجاد شده ارتباط برقرار کند.
    @Override
    protected void processVmCreate(SimEvent ev) {
        int[] data = (int[]) ev.getData();
        int datacenterId = data[0];
        int vmId = data[1];
        int result = data[2];

        if (result == CloudSimTags.TRUE) {
            getVmsToDatacentersMap().put(vmId, datacenterId);
            getVmsCreatedList().add(VmList.getById(getVmList(), vmId));
            Log.printConcatLine(df.format(CloudSim.clock()), ": ", getName(), ": VM #", vmId,
                    " has been created in Datacenter#", datacenterId-1, ", Host #",
                    VmList.getById(getVmsCreatedList(), vmId).getHost().getId());

            /* قطعه برای namenode HDFS اضافه شد */
            // به namenode VM ایجاد شده ارتباط داده شد
            HdfsVm tempVm = VmList.getById(getVmList(), vmId);
            assert tempVm != null;
            int[] tempData;
            //
            if (tempVm.getHdfsType() == CloudSimTags.HDFS_CLIENT) {
                tempData = new int[]{tempVm.getId(), getId()};
                sendNow(nameNodeId, CloudSimTags.HDFS_NAMENODE_ADD_CLIENT, tempData);
            }
            // در صورتی که یک Client VM باشد
            if (tempVm.getHdfsType() == CloudSimTags.HDFS_DN) {
                HdfsHost tempHost = (HdfsHost) tempVm.getHost();
                //ارسال 4مقادیر به NameNode : ش Idگره، datacenterId، RackI، ظرفیت ذخیره سازی
                tempData = new int[]{tempVm.getId(), datacenterId, tempHost.getRackId(), (int) tempHost.getProperStorage().getCapacity()};
                // اطلاعات مربوط به DataNodes را به NameNode ارسال کنید
                sendNow(nameNodeId, CloudSimTags.HDFS_NAMENODE_ADD_DN, tempData);
                // اطلاعات مربوط به DataNodes را به Replication Brokers ارسال کنید
                for (Integer repBrokerId : replicationBrokersId){
                    sendNow(repBrokerId, CloudSimTags.HDFS_REP_BROKER_ADD_DN, tempData);
                }
            }

        } else {
            Log.printConcatLine(df.format(CloudSim.clock()), ": ", getName(), ": Creation of VM #", vmId,
                    " failed in Datacenter#", datacenterId-1);
        }

        incrementVmsAcks();

        // تمام vms درخواستی ایجاد شده اند
        if (getVmsCreatedList().size() == getVmList().size() - getVmsDestroyed()) {

            submitCloudlets();
        } else {
            // همه  acks دریافت شدند، اما برخی از vms ایجاد نشدند
            if (getVmsRequested() == getVmsAcks()) {
                // شDatacenterId بعدی را که امتحان نشده است بیابید
                for (int nextDatacenterId : getDatacenterIdsList()) {
                    if (!getDatacenterRequestedIdsList().contains(nextDatacenterId)) {
                        createVmsInDatacenter(nextDatacenterId);
                        return;
                    }
                }

                // اگرهمه دیتاسنترها قبلاًquery شده اند
                if (getVmsCreatedList().size() > 0) { // اگر مقداری vm ایجاد شده باشد
                    submitCloudlets();
                } else {// وvms ایجاد نشد.
                    Log.printLine(df.format(CloudSim.clock()) + ": " + getName()
                            + ": none of the required VMs could be created. Aborting");
                    finishExecution();
                }
            }
        }
    }

    /**
     * Submit cloudlets to the created VMs.
     *
     * @pre $none
     * @post $none
     * @see #submitCloudletList(java.util.List)
     */
    /**
     * ابر کلودها را به VMهای ایجاد شده ارسال کنید.
     *
     * @pre $ هیچ
     * @post $ هیچ
     * @ مراجعه کنید به #submitCloudletList (java.util.List)
     */
    @Override
    protected void submitCloudlets() {
        int vmIndex = 0;
        List<Cloudlet> successfullySubmitted = new ArrayList<Cloudlet>();
        for (Cloudlet cloudlet : getCloudletList()) {
            Vm vm;
            // اگر کاربر این cloudlet  هنوز اجرا نشده باشد
            if (cloudlet.getVmId() == -1) {
                vm = getVmsCreatedList().get(vmIndex);
            } else { // به vm خاص ارسال کنید
                vm = VmList.getById(getVmsCreatedList(), cloudlet.getVmId());
                if (vm == null) { // وvm ایجاد نشد
                    if(!Log.isDisabled()) {
                        Log.printConcatLine(df.format(CloudSim.clock()), ": ", getName(), ": Postponing execution of Cloudlet ",
                                cloudlet.getCloudletId(), ": bound VM not available");
                    }
                    continue;
                }
            }

            if (!Log.isDisabled()) {
                Log.printConcatLine(df.format(CloudSim.clock()), ": ", getName(), ": Sending Cloudlet ",
                        cloudlet.getCloudletId(), " to VM #", vm.getId());
            }

            cloudlet.setVmId(vm.getId());
            sendNow(getVmsToDatacentersMap().get(vm.getId()), CloudSimTags.HDFS_CLIENT_CLOUDLET_SUBMIT, cloudlet);
            cloudletsSubmitted++;
            currentCloudletMaxId = Math.max(cloudlet.getCloudletId(), currentCloudletMaxId);
            vmIndex = (vmIndex + 1) % getVmsCreatedList().size();
            getCloudletSubmittedList().add(cloudlet);
            successfullySubmitted.add(cloudlet);
        }

        // وCloudlet ارسالی را از لیست انتظار حذف کنید
        getCloudletList().removeAll(successfullySubmitted);
    }

    // تنها تفاوت با submitCloudlets در تگ موجود در sendNow(..) است.
    protected void submitDNCloudlets() {
        int vmIndex = 0;
        List<Cloudlet> successfullySubmitted = new ArrayList<Cloudlet>();
        for (Cloudlet cloudlet : getCloudletList()) {
            Vm vm;
            // اگر کاربر این cloudlet را باند نکرده باشد و هنوز اجرا نشده باشد
            if (cloudlet.getVmId() == -1) {
                vm = getVmsCreatedList().get(vmIndex);
            } else { // به vm خاص ارسال کنید
                vm = VmList.getById(getVmsCreatedList(), cloudlet.getVmId());
                if (vm == null) { //  وvm ایجاد نشد
                    if(!Log.isDisabled()) {
                        Log.printConcatLine(df.format(CloudSim.clock()), ": ", getName(), ": Postponing execution of Data Cloudlet ",
                                cloudlet.getCloudletId(), ": bound VM not available");
                    }
                    continue;
                }
            }

            if (!Log.isDisabled()) {
                Log.printConcatLine(df.format(CloudSim.clock()), ": ", getName(), ": Sending Data Cloudlet ",
                        cloudlet.getCloudletId(), " to VM #", vm.getId());
            }

            // متد باید به طور خودکار Datacenter را که ماشین مجازی DN در آن قرار دارد بدون هیچ مشکلی پیدا کند
            sendNow(getVmsToDatacentersMap().get(vm.getId()), CloudSimTags.HDFS_DN_CLOUDLET_SUBMIT, cloudlet);

            cloudletsSubmitted++;
            currentCloudletMaxId = Math.max(cloudlet.getCloudletId(), currentCloudletMaxId);
            vmIndex = (vmIndex + 1) % getVmsCreatedList().size();
            getCloudletSubmittedList().add(cloudlet);
            successfullySubmitted.add(cloudlet);
        }

        // وCloudlet ارسالی را از لیست انتظار حذف کنید
        getCloudletList().removeAll(successfullySubmitted);
    }

    public int getNameNodeId() {
        return nameNodeId;
    }

    public void setNameNodeId(int nameNodeId) {
        this.nameNodeId = nameNodeId;
    }

    public List<Integer> getReplicationBrokersId() {
        return replicationBrokersId;
    }

    public void setReplicationBrokersId(List<Integer> replicationBrokersId) {
        this.replicationBrokersId = replicationBrokersId;
    }
}
