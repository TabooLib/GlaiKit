package ink.ptms.glaikit.scripting

@Target(AnnotationTarget.FILE)
@Repeatable
@Retention(AnnotationRetention.SOURCE)
annotation class Include(vararg val paths: String)