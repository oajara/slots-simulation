// --------------------------------------------------------------------------
// $Id: Application.java,v 1.3 1997/10/25 18:34:08 ws Exp $
// execution framework for application
//
// (c) 1997, Wolfgang Schreiner <Wolfgang.Schreiner@@risc.uni-linz.ac.at>
// http://www.risc.uni-linz.ac.at/software/daj
// --------------------------------------------------------------------------
package daj;

import daj.awt.Applic;

public abstract class Application extends Applic {

	// --------------------------------------------------------------------------
	// run application with visualization titled 't' and screen of size 'x/y'
	// --------------------------------------------------------------------------
	public Application(String t, int x, int y) {
		super(t, x, y);
	}
}
