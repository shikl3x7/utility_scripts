
lodev=""
mntdir=""

cleanup() {
	     if [ -n "${lodev}" ]; then
		sudo losetup -d "${lodev}"
	     fi
	     if [ -n "${mntdir}" ]; then
		mountpoint "${mntdir}" >/dev/null && sudo umount "${mntdir}"
		rmdir "${mntdir}"
	     fi
     }




# ---------------------------------------------------------------------------------------------------------------------
# Clean previous build
# ---------------------------------------------------------------------------------------------------------------------
get_build_mounts() {
	mount|awk '{print $3;}'|grep ^${PWD}/ || true
}

rm_build_mounts() {
	local tries=0
	while :
	do
	     mounts=$(get_build_mounts)
	     [ -z "${mounts}" ] && break
             for m in ${mounts}
             do
             	sudo umount ${m} 2>/dev/null || true
             done

             # Abort if some mounts remain after multiple attempts
             tries=$((${tries} + 1))

	     if [ ${tries} -ge 3 ]
	     then
	         echo "# error: mounts within 'tmp' are busy!" >&2
	         exit 1
	     fi
	     sleep 1
	done
}

trap cleanup EXIT
mounts=$(get_build_mounts)
if [ -n "${mounts}" ]; then
    echo "# trying to remove mounts from previous build(s)..."
    rm_build_mounts
fi


MACHINE=$1
BUILD_DIRECTORY=$2
echo "$(pwd)"
echo "Printed current working directory"
sudo rm -rf build
. industrial-core/setup-environment ipc
bitbake development-image
bitbake service-stick-image



echo "Create a loopback device for the current service stick image"
sudo losetup -d /dev/loop0 2>/dev/null || true
sudo losetup /dev/loop0 tmp/deploy/images/ipc/service-stick-imageindustrial-os-ipc.wic.img

echo "Probe partitions from the service stick (image)"
sudo partprobe /dev/loop0

echo "Mount the ipc partition"
sudo mkdir -p /mnt/ipc_mnt
sudo mount -o loop /dev/loop0p3 /mnt/ipc_mnt
sudo mkdir -p /mnt/ipc_mnt/images

echo "Copy images to be included"
mv tmp/deploy/images/ipc/development-image-industrial-os-ipc.wic.img tmp/deploy/images/ipc/development-image-industrial-os-ipc.wic
gzip -c tmp/deploy/images/ipc/development-image-industrial-os-ipc.wic | sudo tee /mnt/ipc_mnt/images/image-industrial-os-ipc.wic.gz >/dev/null

echo "Copy the packages you need from the mel-apt folder to the apt folder on the service stick to install offline packages"
sudo umount /mnt/ipc_mnt
sudo losetup -d /dev/loop0
sync

