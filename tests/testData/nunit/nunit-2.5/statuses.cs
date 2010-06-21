using System;
using NUnit.Framework;


  [TestFixture]
  public class Class1
  {
    [Test]
    public void InconclusiveAsAssert()
    {
      Assert.Inconclusive("Shit is here");
    }

    [Test]
    public void InconclusiveAsException()
    {
      throw new InconclusiveException("IOpps");
    }

    [Test, Ignore("Message of ignore")]
    public void IgnoredWithAttribute()
    {
      Assert.Ignore("Shit is here");
    }

    [Test]
    public void IgnoredAsAssert()
    {
      Assert.Ignore("I is here");
    }

    [Test]
    public void PassAsAssert()
    {
      Assert.Pass();
    }

    [Test]
    public void Error()
    {
      throw new Exception("Mega bug");
    }
  }
