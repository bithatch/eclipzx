
package uk.co.bithatch.jspeccy.views;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.HeadlessException;
import java.awt.LayoutManager;
import java.awt.Panel;

import javax.accessibility.Accessible;
import javax.swing.JComponent;
import javax.swing.JLayeredPane;
import javax.swing.JRootPane;
import javax.swing.RootPaneContainer;

@SuppressWarnings("serial")
public class LightweightSwingPanel extends Panel implements Accessible, RootPaneContainer {
	protected JRootPane rootPane;
	protected boolean rootPaneCheckingEnabled = false;

	public LightweightSwingPanel() throws HeadlessException {
		super();

		setForeground(Color.black);
		setBackground(Color.white);

		setLocale(JComponent.getDefaultLocale());
		setLayout(new BorderLayout());
		setRootPane(createRootPane());
		rootPaneCheckingEnabled = true;

		setFocusTraversalPolicyProvider(true);

		enableEvents(AWTEvent.KEY_EVENT_MASK);
	}

	protected JRootPane createRootPane() {
		JRootPane rp = new JRootPane();
		rp.setOpaque(true);
		return rp;
	}

	@Override
	public void update(Graphics g) {
		paint(g);
	}

	@Override
	protected void addImpl(Component comp, Object constraints, int index) {
		if (rootPaneCheckingEnabled) {
			getContentPane().add(comp, constraints, index);
		} else {
			super.addImpl(comp, constraints, index);
		}
	}

	@Override
	public void remove(Component comp) {
		if (comp == rootPane) {
			super.remove(comp);
		} else {
			getContentPane().remove(comp);
		}
	}

	@Override
	public void setLayout(LayoutManager manager) {
		if (rootPaneCheckingEnabled) {
			getContentPane().setLayout(manager);
		} else {
			super.setLayout(manager);
		}
	}

	@Override
	public JRootPane getRootPane() {
		return rootPane;
	}

	protected void setRootPane(JRootPane root) {
		if (rootPane != null) {
			remove(rootPane);
		}
		rootPane = root;
		if (rootPane != null) {
			boolean checkingEnabled = this.rootPaneCheckingEnabled;
			try {
				rootPaneCheckingEnabled = false;
				add(rootPane, BorderLayout.CENTER);
			} finally {
				rootPaneCheckingEnabled = checkingEnabled;
			}
		}
	}

	@Override
	public Container getContentPane() {
		return getRootPane().getContentPane();
	}

	@Override
	public void setContentPane(Container contentPane) {
		getRootPane().setContentPane(contentPane);
	}

	@Override
	public JLayeredPane getLayeredPane() {
		return getRootPane().getLayeredPane();
	}

	@Override
	public void setLayeredPane(JLayeredPane layeredPane) {
		getRootPane().setLayeredPane(layeredPane);
	}

	@Override
	public Component getGlassPane() {
		return getRootPane().getGlassPane();
	}

	@Override
	public void setGlassPane(Component glassPane) {
		getRootPane().setGlassPane(glassPane);
	}

}
