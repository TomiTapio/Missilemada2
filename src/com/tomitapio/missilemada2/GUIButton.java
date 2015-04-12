package com.tomitapio.missilemada2;

import org.lwjgl.opengl.GL11;
import org.newdawn.slick.Color;

/**
 * Date: 24.5.2014
 */
public class GUIButton {
  String content = "ui";
  private int button_id;
  private double xplace;
  private double yplace;
  private double z;
  private double xplace_text;
  private double yplace_text;
  private float wid, hei, textscale;
  private boolean mode_is_active_visualstate = false;
  private boolean can_afford_build = false;
  private boolean drawclick = false;
  private long expiry_timestamp = 0;

  public GUIButton(int id_in, String c, double xplacei, double yplacei,

                   double in_width, double in_height,  double z_in, float scale_in, float transp_notused_defa00) {
    content = c;
    button_id = id_in;

    xplace = xplacei;
    yplace = yplacei;
    z = z_in;
    wid = (float)in_width; //pixels
    hei = (float)in_height;
    textscale = scale_in;

    xplace_text = xplacei;
    yplace_text = yplacei;
  }
  public void drawText(Color col) {
    //are in HUD-drawing coords
    //can't use Missilemada2.drawTextXYZ coz that one is for while-in-3d-space
    Missilemada2.drawTextHUD(Missilemada2.getFont60fornowwww(0), textscale, (float) xplace, (float) yplace-2.0f, content, col);
  }
  public void draw_parts() {
    float zcoord = 0.0f;
    //are in HUD-drawing coords

    //draw quad, diff color if currently active choice
    if (mode_is_active_visualstate) {
      GL11.glColor4f(0.96f, 0.96f, 0.3f, 0.8f); //yellow
    } else {
      if (can_afford_build) {
        GL11.glColor4f(0.2f, 0.76f, 0.2f, 0.8f); //light green, can afford
      } else {
        //not active mode, and/or can't afford.
        GL11.glColor4f(0.26f, 0.26f, 0.7f, 0.8f); //blue
      }
    }


    //draw pretty border? nah.

    if (drawclick) {
      GL11.glColor4f(0.93f, 0.93f, 0.93f, 0.99f); //white
      GL11.glBegin(GL11.GL_QUADS);
      GL11.glTexCoord2f(0, 0);
      GL11.glVertex3f((float)xplace-1, (float)yplace-1, (float)zcoord);          GL11.glNormal3f(0.0f, 0.0f, 1.0f);
      GL11.glTexCoord2f(0, 22);
      GL11.glVertex3f((float)xplace+wid+1, (float)yplace-1, (float)zcoord);          GL11.glNormal3f(0.0f, 0.0f, 1.0f);
      GL11.glTexCoord2f(22,22);
      GL11.glVertex3f((float)xplace+wid+1, (float)yplace+hei+1, (float)zcoord);      GL11.glNormal3f(0.0f, 0.0f, 1.0f);
      GL11.glTexCoord2f(0,22);
      GL11.glVertex3f((float)xplace-1, (float)yplace+hei+1, (float)zcoord);          GL11.glNormal3f(0.0f, 0.0f, 1.0f);
      GL11.glEnd();

      if (Missilemada2.getWorldTime() > expiry_timestamp) {
        drawclick = false;
        expiry_timestamp = 0;
      }
    } else {
      //draw quad of button
      GL11.glBegin(GL11.GL_QUADS);
      GL11.glTexCoord2f(0, 0);
      GL11.glVertex3f((float)xplace, (float)yplace, (float)zcoord);          GL11.glNormal3f(0.0f, 0.0f, 1.0f);
      GL11.glTexCoord2f(0, 22);
      GL11.glVertex3f((float)xplace+wid, (float)yplace, (float)zcoord);          GL11.glNormal3f(0.0f, 0.0f, 1.0f);
      GL11.glTexCoord2f(22,22);
      GL11.glVertex3f((float)xplace+wid, (float)yplace+hei, (float)zcoord);      GL11.glNormal3f(0.0f, 0.0f, 1.0f);
      GL11.glTexCoord2f(0,22);
      GL11.glVertex3f((float)xplace, (float)yplace+hei, (float)zcoord);          GL11.glNormal3f(0.0f, 0.0f, 1.0f);
      GL11.glEnd();
    }

  }

  public int getButtonId() {
    return button_id;
  }

  public boolean areMouseCoordsInsideYou(int mouse_x, int mouse_y) {
    if ( mouse_x > xplace && mouse_x < xplace + wid) {
      if ( mouse_y > yplace && mouse_y < yplace + hei) {
        //System.out.println("GUIButton click, button_id "+button_id+", coords "+mouse_x+ ", "+mouse_y);
        return true;
      }
    }
    return false;
  }

  public void setVisualState(boolean b) {
    mode_is_active_visualstate = b;
  }
  public boolean isAffordable() {
    return can_afford_build;
  }
  public void setCanAfford(boolean in_can_afford_build) {
    can_afford_build = in_can_afford_build;
  }

  public void setClickFxOn(long expirytimestamp) {
    drawclick = true;
    expiry_timestamp = expirytimestamp;
  }
}
