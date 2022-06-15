package com.example.coffeeproject.ui.outletregistration.responses.initiallistapiresponse

data class AllList(
    val BrandList: List<Brand>,
    val DesignationList: List<Designation>,
    val MachineList: List<Machine>,
    val SyrupList: List<Syrup>
)