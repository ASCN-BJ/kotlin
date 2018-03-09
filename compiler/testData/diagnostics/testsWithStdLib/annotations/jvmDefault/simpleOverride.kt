// !DIAGNOSTICS: -EXPERIMENTAL_API_USAGE
// !API_VERSION: 1.3
// !JVM_TARGET: 1.8
interface A {
    @JvmDefault
    fun test() {}
}

interface Abstract : A {
    <!JVM_DEFAULT_REQUIRED_FOR_OVERRIDE!>override fun test()<!>
}

interface ANonDefault {
    fun test() {}
}

interface B: A {
    <!JVM_DEFAULT_REQUIRED_FOR_OVERRIDE!>override fun test()<!> {}
}

interface C: ANonDefault, A {
    <!JVM_DEFAULT_REQUIRED_FOR_OVERRIDE!>override fun test()<!> {}
}

interface D: A, ANonDefault {
    <!JVM_DEFAULT_REQUIRED_FOR_OVERRIDE!>override fun test()<!> {}
}