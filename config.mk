# Working Space — portable focus session module
PRODUCT_PACKAGES += \
    WorkingSpace \
    privapp_whitelist_com.bmobile.workingspace.xml

PRODUCT_SOONG_NAMESPACES += \
    vendor/bmobile/workingspace \
    vendor/bmobile/workingspace/third_party/appintro

SYSTEM_EXT_PRIVATE_SEPOLICY_DIRS += \
    vendor/bmobile/workingspace/sepolicy/private
