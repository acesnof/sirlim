package pt.sirlim.app.data.model

import kotlinx.serialization.Serializable
import pt.sirlim.app.ui.screens.admin.groups_compartments.CompartmentTask
import pt.sirlim.app.ui.screens.admin.indications.IndicationTask
import pt.sirlim.app.ui.screens.admin.indications.IndicationUser
import pt.sirlim.app.ui.screens.user.consultations.CleaningPerformedTask

@Serializable
data class SirlimBackup(
    val exportDate: String,
    val users: List<User>,
    val groups: List<Group>,
    val compartments: List<Compartment>,
    val tasks: List<Task>,
    val cleanings: List<Cleaning>,
    val indications: List<Indication>,
    val indicationUsers: List<IndicationUser>,
    val indicationTasks: List<IndicationTask>,
    val compartmentTasks: List<CompartmentTask>,
    val cleaningPerformedTasks: List<CleaningPerformedTask>
)
