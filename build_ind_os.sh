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

