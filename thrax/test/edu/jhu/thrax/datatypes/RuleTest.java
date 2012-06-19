package edu.jhu.thrax.datatypes;

import org.testng.Assert;
import org.testng.annotations.Test;

public class RuleTest {

    private Rule a;
    private Rule b;

    @Test
    public void Ctor_Equals()
    {
        int [] aSen = new int [] { 0, 0, 1, 2, 3, 3, 4 };
        int [] bSen = new int [] { 0, 1, 2, 2, 2, 3, 3, 4, 4, 4 };
        Alignment aAl = new Alignment("0-0 1-1 2-2 3-3 4-4 5-5 6-6");
        Alignment bAl = new Alignment("0-0 1-1 2-2 3-3 4-4 5-5 6-6 7-7 8-8 9-9");
        a = new Rule(aSen, aSen, aAl, 0, 2);
        b = new Rule(bSen, bSen, bAl, 0, 2);

        Assert.assertTrue(a.equals(b));
        Assert.assertTrue(b.equals(a));
        Assert.assertEquals(a.hashCode(), b.hashCode());
    }

    @Test(dependsOnMethods = { "Ctor_Equals" })
    public void Equal_AfterBExtendTerm_False()
    {
        b.extendWithTerminal();
        Assert.assertFalse(a.equals(b));
        Assert.assertFalse(b.equals(a));
    }

    @Test(dependsOnMethods = { "Equal_AfterBExtendTerm_False" })
    public void Equal_AfterAExtendTerm_True()
    {
        a.extendWithTerminal();
        Assert.assertTrue(a.equals(b));
        Assert.assertTrue(b.equals(a));
        Assert.assertEquals(a.hashCode(), b.hashCode());
    }

}
