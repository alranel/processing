/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2011-12 Ben Fry and Casey Reas

  This library is free software; you can redistribute it and/or
  modify it under the terms of the GNU Lesser General Public
  License as published by the Free Software Foundation; either
  version 2.1 of the License, or (at your option) any later version.

  This library is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  Lesser General Public License for more details.

  You should have received a copy of the GNU Lesser General
  Public License along with this library; if not, write to the
  Free Software Foundation, Inc., 59 Temple Place, Suite 330,
  Boston, MA  02111-1307  USA
*/

package processing.opengl;

import java.awt.BorderLayout;
import java.awt.Canvas;
import java.nio.Buffer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GL2ES2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLCapabilitiesImmutable;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLDrawable;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLProfile;
import javax.media.opengl.awt.GLCanvas;
import javax.media.opengl.glu.GLU;
import javax.media.opengl.glu.GLUtessellator;
import javax.media.opengl.glu.GLUtessellatorCallbackAdapter;

import com.jogamp.newt.awt.NewtCanvasAWT;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.util.AnimatorBase;

/**
 * Processing-OpenGL abstraction layer.
 *
 */
public class PGL {
  // The two windowing toolkits available to use in JOGL:
  public static final int AWT  = 0; // http://jogamp.org/wiki/index.php/Using_JOGL_in_AWT_SWT_and_Swing
  public static final int NEWT = 1; // http://jogamp.org/jogl/doc/NEWT-Overview.html

  /** Size of a short (in bytes). */
  public static final int SIZEOF_SHORT = Short.SIZE / 8;

  /** Size of an int (in bytes). */
  public static final int SIZEOF_INT = Integer.SIZE / 8;

  /** Size of a float (in bytes). */
  public static final int SIZEOF_FLOAT = Float.SIZE / 8;

  /** Size of a byte (in bytes). */
  public static final int SIZEOF_BYTE = Byte.SIZE / 8;
  
  /** Size of a vertex index. */
  public static final int SIZEOF_INDEX = SIZEOF_SHORT;

  /** Type of a vertex index. */
  public static final int INDEX_TYPE = GL.GL_UNSIGNED_SHORT;

  /** Initial sizes for arrays of input and tessellated data. */
  public static final int DEFAULT_IN_VERTICES   = 64;
  public static final int DEFAULT_IN_EDGES      = 128;
  public static final int DEFAULT_IN_TEXTURES   = 64;
  public static final int DEFAULT_TESS_VERTICES = 64;
  public static final int DEFAULT_TESS_INDICES  = 128;

  /** Maximum lights by default is 8, the minimum defined by OpenGL. */
  public static final int MAX_LIGHTS = 8;

  /** Maximum index value of a tessellated vertex. GLES restricts the vertex 
   * indices to be of type unsigned short. Since Java only supports signed
   * shorts as primitive type we have 2^15 = 32768 as the maximum number of  
   * vertices that can be referred to within a single VBO. */
  public static final int MAX_VERTEX_INDEX  = 32767;
  public static final int MAX_VERTEX_INDEX1 = MAX_VERTEX_INDEX + 1;
  
  /** Count of tessellated fill, line or point vertices that will 
   * trigger a flush in the immediate mode. It doesn't necessarily 
   * be equal to MAX_VERTEX_INDEX1, since the number of vertices can 
   * be effectively much large since the renderer uses offsets to
   * refer to vertices beyond the MAX_VERTEX_INDEX limit. 
   */
  public static final int FLUSH_VERTEX_COUNT = MAX_VERTEX_INDEX1; 
  
  /** Maximum dimension of a texture used to hold font data. **/
  public static final int MAX_FONT_TEX_SIZE = 1024;

  /** Minimum stroke weight needed to apply the full path stroking
   * algorithm that properly generates caps and joins. 
   */
  public static final float MIN_CAPS_JOINS_WEIGHT = 1.5f;
  
  /** Maximum length of linear paths to be stroked with the 
   * full algorithm that generates accurate caps and joins. 
   */  
  public static final int MAX_CAPS_JOINS_LENGTH = 5000;
  
  /** Minimum array size to use arrayCopy method(). **/
  protected static final int MIN_ARRAYCOPY_SIZE = 2;

  /** Enables/disables mipmap use. **/
  protected static final boolean MIPMAPS_ENABLED = true;  
  
  /** Machine Epsilon for float precision. **/
  public static float FLOAT_EPS = Float.MIN_VALUE;
  // Calculation of the Machine Epsilon for float precision. From:
  // http://en.wikipedia.org/wiki/Machine_epsilon#Approximation_using_Java
  static {
    float eps = 1.0f;

    do {
      eps /= 2.0f;
    } while ((float)(1.0 + (eps / 2.0)) != 1.0);

    FLOAT_EPS = eps;
  }
  
  /**
   * Set to true if the host system is big endian (PowerPC, MIPS, SPARC), false
   * if little endian (x86 Intel for Mac or PC).
   */
  public static boolean BIG_ENDIAN = ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN;

  protected static final String SHADER_PREPROCESSOR_DIRECTIVE = "#ifdef GL_ES\n" +
                                                                "precision mediump float;\n" +
                                                                "precision mediump int;\n" +
                                                                "#endif\n";

  ///////////////////////////////////////////////////////////////////////////////////

  // OpenGL constants

  public static final int GL_FALSE = GL.GL_FALSE;
  public static final int GL_TRUE  = GL.GL_TRUE;

  public static final int GL_LESS      = GL.GL_LESS;
  public static final int GL_LEQUAL    = GL.GL_LEQUAL;
  
  public static final int GL_CCW       = GL.GL_CCW;
  public static final int GL_CW        = GL.GL_CW;
  
  public static final int GL_CULL_FACE      = GL.GL_CULL_FACE;
  public static final int GL_FRONT          = GL.GL_FRONT;
  public static final int GL_BACK           = GL.GL_BACK;
  public static final int GL_FRONT_AND_BACK = GL.GL_FRONT_AND_BACK;

  public static final int GL_VIEWPORT = GL.GL_VIEWPORT;

  public static final int GL_SCISSOR_TEST    = GL.GL_SCISSOR_TEST;
  public static final int GL_DEPTH_TEST      = GL.GL_DEPTH_TEST;
  public static final int GL_DEPTH_WRITEMASK = GL.GL_DEPTH_WRITEMASK;

  public static final int GL_COLOR_BUFFER_BIT   = GL.GL_COLOR_BUFFER_BIT;
  public static final int GL_DEPTH_BUFFER_BIT   = GL.GL_DEPTH_BUFFER_BIT;
  public static final int GL_STENCIL_BUFFER_BIT = GL.GL_STENCIL_BUFFER_BIT;

  public static final int GL_FUNC_ADD              = GL.GL_FUNC_ADD;
  public static final int GL_FUNC_MIN              = GL2.GL_MIN;
  public static final int GL_FUNC_MAX              = GL2.GL_MAX;
  public static final int GL_FUNC_REVERSE_SUBTRACT = GL.GL_FUNC_REVERSE_SUBTRACT;

  public static final int GL_TEXTURE_2D        = GL.GL_TEXTURE_2D;
  public static final int GL_TEXTURE_RECTANGLE = GL2.GL_TEXTURE_RECTANGLE;
  
  public static final int GL_RGB            = GL.GL_RGB;
  public static final int GL_RGBA           = GL.GL_RGBA;
  public static final int GL_ALPHA          = GL.GL_ALPHA;
  public static final int GL_UNSIGNED_INT   = GL.GL_UNSIGNED_INT;
  public static final int GL_UNSIGNED_BYTE  = GL.GL_UNSIGNED_BYTE;
  public static final int GL_UNSIGNED_SHORT = GL.GL_UNSIGNED_SHORT;
  public static final int GL_FLOAT          = GL.GL_FLOAT;

  public static final int GL_NEAREST               = GL.GL_NEAREST;
  public static final int GL_LINEAR                = GL.GL_LINEAR;
  public static final int GL_LINEAR_MIPMAP_NEAREST = GL.GL_LINEAR_MIPMAP_NEAREST;
  public static final int GL_LINEAR_MIPMAP_LINEAR  = GL.GL_LINEAR_MIPMAP_LINEAR;  
  
  public static final int GL_CLAMP_TO_EDGE = GL.GL_CLAMP_TO_EDGE;
  public static final int GL_REPEAT        = GL.GL_REPEAT;

  public static final int GL_RGBA8            = GL.GL_RGBA8;
  public static final int GL_DEPTH24_STENCIL8 = GL.GL_DEPTH24_STENCIL8;

  public static final int GL_DEPTH_COMPONENT   = GL2.GL_DEPTH_COMPONENT;
  public static final int GL_DEPTH_COMPONENT16 = GL.GL_DEPTH_COMPONENT16;
  public static final int GL_DEPTH_COMPONENT24 = GL.GL_DEPTH_COMPONENT24;
  public static final int GL_DEPTH_COMPONENT32 = GL.GL_DEPTH_COMPONENT32;

  public static final int GL_STENCIL_INDEX  = GL2.GL_STENCIL_INDEX;
  public static final int GL_STENCIL_INDEX1 = GL.GL_STENCIL_INDEX1;
  public static final int GL_STENCIL_INDEX4 = GL.GL_STENCIL_INDEX4;
  public static final int GL_STENCIL_INDEX8 = GL.GL_STENCIL_INDEX8;

  public static final int GL_ARRAY_BUFFER         = GL.GL_ARRAY_BUFFER;
  public static final int GL_ELEMENT_ARRAY_BUFFER = GL.GL_ELEMENT_ARRAY_BUFFER;

  public static final int GL_SAMPLES = GL.GL_SAMPLES;

  public static final int GL_FRAMEBUFFER_COMPLETE                      = GL.GL_FRAMEBUFFER_COMPLETE;
  public static final int GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT         = GL.GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT;
  public static final int GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT = GL.GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT;
  public static final int GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS         = GL.GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS;
  public static final int GL_FRAMEBUFFER_INCOMPLETE_FORMATS            = GL.GL_FRAMEBUFFER_INCOMPLETE_FORMATS;
  public static final int GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER        = GL2.GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER;
  public static final int GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER        = GL2.GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER;
  public static final int GL_FRAMEBUFFER_UNSUPPORTED                   = GL.GL_FRAMEBUFFER_UNSUPPORTED;

  public static final int GL_STATIC_DRAW  = GL.GL_STATIC_DRAW;
  public static final int GL_DYNAMIC_DRAW = GL.GL_DYNAMIC_DRAW;
  public static final int GL_STREAM_DRAW  = GL2.GL_STREAM_DRAW;

  public static final int GL_READ_ONLY  = GL2.GL_READ_ONLY;
  public static final int GL_WRITE_ONLY = GL2.GL_WRITE_ONLY;
  public static final int GL_READ_WRITE = GL2.GL_READ_WRITE;

  public static final int GL_TRIANGLE_FAN   = GL.GL_TRIANGLE_FAN;
  public static final int GL_TRIANGLE_STRIP = GL.GL_TRIANGLE_STRIP;
  public static final int GL_TRIANGLES      = GL.GL_TRIANGLES;

  public static final int GL_VENDOR                   = GL.GL_VENDOR;
  public static final int GL_RENDERER                 = GL.GL_RENDERER;
  public static final int GL_VERSION                  = GL.GL_VERSION;
  public static final int GL_EXTENSIONS               = GL.GL_EXTENSIONS;
  public static final int GL_SHADING_LANGUAGE_VERSION = GL2ES2.GL_SHADING_LANGUAGE_VERSION;

  public static final int GL_MAX_TEXTURE_SIZE         = GL.GL_MAX_TEXTURE_SIZE;
  public static final int GL_MAX_SAMPLES              = GL2.GL_MAX_SAMPLES;
  public static final int GL_ALIASED_LINE_WIDTH_RANGE = GL.GL_ALIASED_LINE_WIDTH_RANGE;
  public static final int GL_ALIASED_POINT_SIZE_RANGE = GL.GL_ALIASED_POINT_SIZE_RANGE;
  public static final int GL_DEPTH_BITS               = GL.GL_DEPTH_BITS;
  public static final int GL_STENCIL_BITS             = GL.GL_STENCIL_BITS;

  public static final int GLU_TESS_WINDING_NONZERO = GLU.GLU_TESS_WINDING_NONZERO;
  public static final int GLU_TESS_WINDING_ODD     = GLU.GLU_TESS_WINDING_ODD;

  public static final int GL_TEXTURE0           = GL.GL_TEXTURE0;
  public static final int GL_TEXTURE1           = GL.GL_TEXTURE1;
  public static final int GL_TEXTURE2           = GL.GL_TEXTURE2;
  public static final int GL_TEXTURE3           = GL.GL_TEXTURE3;
  public static final int GL_TEXTURE_MIN_FILTER = GL.GL_TEXTURE_MIN_FILTER;
  public static final int GL_TEXTURE_MAG_FILTER = GL.GL_TEXTURE_MAG_FILTER;
  public static final int GL_TEXTURE_WRAP_S     = GL.GL_TEXTURE_WRAP_S;
  public static final int GL_TEXTURE_WRAP_T     = GL.GL_TEXTURE_WRAP_T;

  public static final int GL_BLEND               = GL.GL_BLEND;
  public static final int GL_ONE                 = GL.GL_ONE;
  public static final int GL_ZERO                = GL.GL_ZERO;
  public static final int GL_SRC_ALPHA           = GL.GL_SRC_ALPHA;
  public static final int GL_DST_ALPHA           = GL.GL_DST_ALPHA;
  public static final int GL_ONE_MINUS_SRC_ALPHA = GL.GL_ONE_MINUS_SRC_ALPHA;
  public static final int GL_ONE_MINUS_DST_COLOR = GL.GL_ONE_MINUS_DST_COLOR;
  public static final int GL_ONE_MINUS_SRC_COLOR = GL.GL_ONE_MINUS_SRC_COLOR;
  public static final int GL_DST_COLOR           = GL.GL_DST_COLOR;
  public static final int GL_SRC_COLOR           = GL.GL_SRC_COLOR;

  public static final int GL_FRAMEBUFFER        = GL.GL_FRAMEBUFFER;
  public static final int GL_COLOR_ATTACHMENT0  = GL.GL_COLOR_ATTACHMENT0;
  public static final int GL_COLOR_ATTACHMENT1  = GL2.GL_COLOR_ATTACHMENT1;
  public static final int GL_COLOR_ATTACHMENT2  = GL2.GL_COLOR_ATTACHMENT2;
  public static final int GL_COLOR_ATTACHMENT3  = GL2.GL_COLOR_ATTACHMENT3;
  public static final int GL_RENDERBUFFER       = GL.GL_RENDERBUFFER;
  public static final int GL_DEPTH_ATTACHMENT   = GL.GL_DEPTH_ATTACHMENT;
  public static final int GL_STENCIL_ATTACHMENT = GL.GL_STENCIL_ATTACHMENT;
  public static final int GL_READ_FRAMEBUFFER   = GL2.GL_READ_FRAMEBUFFER;
  public static final int GL_DRAW_FRAMEBUFFER   = GL2.GL_DRAW_FRAMEBUFFER;

  public static final int GL_VERTEX_SHADER        = GL2.GL_VERTEX_SHADER;
  public static final int GL_FRAGMENT_SHADER      = GL2.GL_FRAGMENT_SHADER;
  public static final int GL_INFO_LOG_LENGTH      = GL2.GL_INFO_LOG_LENGTH;
  public static final int GL_SHADER_SOURCE_LENGTH = GL2.GL_SHADER_SOURCE_LENGTH;
  public static final int GL_COMPILE_STATUS       = GL2.GL_COMPILE_STATUS;
  public static final int GL_LINK_STATUS          = GL2.GL_LINK_STATUS;
  public static final int GL_VALIDATE_STATUS      = GL2.GL_VALIDATE_STATUS;

  public static final int GL_MULTISAMPLE    = GL.GL_MULTISAMPLE;
  public static final int GL_POINT_SMOOTH   = GL2.GL_POINT_SMOOTH;
  public static final int GL_LINE_SMOOTH    = GL.GL_LINE_SMOOTH;
  public static final int GL_POLYGON_SMOOTH = GL2.GL_POLYGON_SMOOTH;

  /** Basic GL functionality, common to all profiles */
  public GL gl;

  /** GLES2 functionality (shaders, etc) */
  public GL2ES2 gl2;

  /** GL2 desktop functionality (blit framebuffer, map buffer range, multisampled renerbuffers) */
  public GL2 gl2x;

  /** GLU interface **/
  public GLU glu;

  /** The PGraphics object using this interface */
  public PGraphicsOpenGL pg;

  /** Whether OpenGL has been initialized or not */
  public boolean initialized;

  /** Windowing toolkit */ 
  public static int toolkit = AWT;
  
  /** Selected GL profile */
  public GLProfile profile;

  /** The capabilities of the OpenGL rendering surface */
  public GLCapabilitiesImmutable capabilities;

  /** The rendering surface */
  public GLDrawable drawable;

  /** The rendering context (holds rendering state info) */
  public GLContext context;

  /** The AWT canvas where OpenGL rendering takes place */
  public Canvas canvas;

  /** The AWT-OpenGL canvas */
  protected GLCanvas canvasAWT;

  /** The NEWT-OpenGL canvas */
  protected NewtCanvasAWT canvasNEWT;

  /** The NEWT window */
  protected GLWindow window;

  /** The listener that fires the frame rendering in Processing */
  protected PGLListener listener;

  /** Animator to drive the rendering thread in NEWT */
  protected PGLAnimator animator;

  /** Desired target framerate */
  protected float targetFramerate = 60;
  protected boolean setFramerate = false;

  ///////////////////////////////////////////////////////////////////////////////////
  
  // FBO for anti-aliased rendering  
  
  public static final boolean ENABLE_OSX_SCREEN_FBO  = true;
  public static final int MIN_OSX_VER_FOR_SCREEN_FBO = 6;
  public static final int MIN_SAMPLES_FOR_SCREEN_FBO = 1;  
  protected boolean needScreenFBO = false;
  protected int fboWidth, fboHeight;  
  protected int numSamples;
  protected boolean multisample;
  protected boolean packedDepthStencil;
  protected int[] glColorTexID = { 0 };
  protected int[] glColorFboID = { 0 };
  protected int[] glMultiFboID = { 0 };
  protected int[] glColorRenderBufferID = { 0 };
  protected int[] glPackedDepthStencilID = { 0 };
  protected int[] glDepthBufferID = { 0 };
  protected int[] glStencilBufferID = { 0 };
  protected int contextHashCode;
  
  ///////////////////////////////////////////////////////////////////////////////////

  // Texture rendering

  protected boolean loadedTex2DShader = false;
  protected int tex2DShaderProgram;
  protected int tex2DVertShader;
  protected int tex2DFragShader;
  protected GLContext tex2DShaderContext;
  protected int tex2DVertLoc;
  protected int tex2DTCoordLoc;

  protected boolean loadedTexRectShader = false;
  protected int texRectShaderProgram;
  protected int texRectVertShader;
  protected int texRectFragShader;
  protected GLContext texRectShaderContext;
  protected int texRectVertLoc;
  protected int texRectTCoordLoc; 
  
  protected float[] texCoords = {
    //  X,     Y,    U,    V
    -1.0f, -1.0f, 0.0f, 0.0f,
    +1.0f, -1.0f, 1.0f, 0.0f,
    -1.0f, +1.0f, 0.0f, 1.0f,
    +1.0f, +1.0f, 1.0f, 1.0f
  };
  protected FloatBuffer texData;

  protected String texVertShaderSource = "attribute vec2 inVertex;" +
                                         "attribute vec2 inTexcoord;" +
                                         "varying vec2 vertTexcoord;" +
                                         "void main() {" +
                                         "  gl_Position = vec4(inVertex, 0, 1);" +
                                         "  vertTexcoord = inTexcoord;" +
                                         "}";

  protected String tex2DFragShaderSource = SHADER_PREPROCESSOR_DIRECTIVE +
                                           "uniform sampler2D textureSampler;" +
                                           "varying vec2 vertTexcoord;" +
                                           "void main() {" +
                                           "  gl_FragColor = texture2D(textureSampler, vertTexcoord.st);" +
                                           "}";

  protected String texRectFragShaderSource = SHADER_PREPROCESSOR_DIRECTIVE +
                                             "uniform sampler2DRect textureSampler;" +
                                             "varying vec2 vertTexcoord;" +
                                             "void main() {" +
                                             "  gl_FragColor = texture2DRect(textureSampler, vertTexcoord.st);" +
                                             "}";
  
  ///////////////////////////////////////////////////////////////////////////////////

  // Rectangle rendering

  protected boolean loadedRectShader = false;
  protected int rectShaderProgram;
  protected int rectVertShader;
  protected int rectFragShader;
  protected GLContext rectShaderContext;

  protected int rectVertLoc;
  protected int rectColorLoc;

  protected float[] rectCoords = {
    //  X,     Y
    -1.0f, -1.0f,
    +1.0f, -1.0f,
    -1.0f, +1.0f,
    +1.0f, +1.0f,
  };
  protected FloatBuffer rectData;

  protected String rectVertShaderSource = "attribute vec2 inVertex;" +
                                          "void main() {" +
                                          "  gl_Position = vec4(inVertex, 0, 1);" +
                                          "}";

  protected String rectFragShaderSource = SHADER_PREPROCESSOR_DIRECTIVE +
                                          "uniform vec4 rectColor;" +
                                          "void main() {" +
                                          "  gl_FragColor = rectColor;" +
                                          "}";

  ///////////////////////////////////////////////////////////////////////////////////

  // 1-pixel color, depth, stencil buffers

  protected IntBuffer colorBuffer;
  protected FloatBuffer depthBuffer;
  protected ByteBuffer stencilBuffer;


  ///////////////////////////////////////////////////////////////////////////////////

  // Intialization, finalization


  public PGL(PGraphicsOpenGL pg) {
    this.pg = pg;
    glu = new GLU();
    initialized = false;
  }


  public void setFramerate(float framerate) {
    if (targetFramerate != framerate) {
      if (60 < framerate) {
        // Disables v-sync
        gl.setSwapInterval(0);
      } else if (30 < framerate) {
        gl.setSwapInterval(1);
      } else {
        gl.setSwapInterval(2);
      }
      if ((60 < framerate && targetFramerate <= 60) ||
          (framerate <= 60 && 60 < targetFramerate)) {
        // Enabling/disabling v-sync, we force a
        // surface reinitialization to avoid screen
        // no-paint issue observed on MacOSX.
        initialized = false;
      }
      targetFramerate = framerate;
      setFramerate = true;
    }
  }


  public void setToolkit(int toolkit) {
    if (PGL.toolkit != toolkit) {
      PGL.toolkit = toolkit;
      this.initialized = false;
    }
  }
  
  
  public void initPrimarySurface(int antialias) {
    if (ENABLE_OSX_SCREEN_FBO) {
      needScreenFBO = false;
      glColorFboID[0] = 0;  
      String osName = System.getProperty("os.name");
      if (osName.equals("Mac OS X")) {
        String version = System.getProperty("os.version");
        String[] parts = version.split("\\.");
        if (2 <= parts.length) {
          int num = Integer.parseInt(parts[1]);
          if (MIN_OSX_VER_FOR_SCREEN_FBO <= num && 
              MIN_SAMPLES_FOR_SCREEN_FBO <= qualityToSamples(pg.quality)) {
            // Using an FBO for screen drawing works better than the
            // screen framebuffer.
            // This fixes the problem of antialiasing on Lion or newer,
            // the flickering associated to glReadPixels calls on             
            // 10.6+, and it is in fact faster.
            needScreenFBO = true;  
          }
        }
      } 
    }
    
    if (profile == null) {
      profile = GLProfile.getDefault();
    } else {
      // Restarting...
      if (canvasAWT != null) {
        // TODO: Even if the GLCanvas is put inside an animator, the rendering runs 
        // inside the EDT, ask the JOGL guys about this.
//        animator.stop();
//        animator.remove(canvasAWT);        
        canvasAWT.removeGLEventListener(listener);
        pg.parent.removeListeners(canvasAWT);
        pg.parent.remove(canvasAWT);
      } else if (canvasNEWT != null) {
        animator.stop();
        animator.remove(window);
        window.removeGLEventListener(listener);
        pg.parent.removeListeners(canvasNEWT);
        pg.parent.remove(canvasNEWT);
      }
      setFramerate = false;
    }

    // Setting up the desired GL capabilities;
    GLCapabilities caps = new GLCapabilities(profile);
    if (1 < antialias && !needScreenFBO) {
      caps.setSampleBuffers(true);
      caps.setNumSamples(antialias);
    } else {
      caps.setSampleBuffers(false);
    }
    caps.setDepthBits(24);
    caps.setStencilBits(8);
    caps.setAlphaBits(8);
    caps.setBackgroundOpaque(true);

    if (toolkit == AWT) {
      canvasAWT = new GLCanvas(caps);
      canvasAWT.setBounds(0, 0, pg.width, pg.height);

      pg.parent.setLayout(new BorderLayout());
      pg.parent.add(canvasAWT, BorderLayout.CENTER);
      pg.parent.removeListeners(pg.parent);
      pg.parent.addListeners(canvasAWT);

      listener = new PGLListener();
      canvasAWT.addGLEventListener(listener);
//      animator = new PGLAnimator(canvasAWT);
//      animator.start();
      
      capabilities = canvasAWT.getChosenGLCapabilities();
      canvas = canvasAWT;
      canvasNEWT = null;
    } else if (toolkit == NEWT) {
      window = GLWindow.create(caps);
      canvasNEWT = new NewtCanvasAWT(window);

      pg.parent.setLayout(new BorderLayout());
      pg.parent.add(canvasNEWT, BorderLayout.CENTER);
      pg.parent.removeListeners(pg.parent);
      pg.parent.addListeners(canvasNEWT);

      listener = new PGLListener();
      window.addGLEventListener(listener);
      animator = new PGLAnimator(window);
      animator.start();

      capabilities = window.getChosenGLCapabilities();
      canvas = canvasNEWT;
      canvasAWT = null;
    }

    initialized = true;
  }


  public void initOffscreenSurface(PGL primary) {
    context = primary.context;
    capabilities = primary.capabilities;
    drawable = null;
    initialized = true;
  }


  public void updatePrimary() {
    if (!setFramerate) {
      setFramerate(targetFramerate);
    }
    
    if (needScreenFBO && glColorFboID[0] == 0) {
      numSamples = qualityToSamples(pg.quality);      
      
      String ext = gl.glGetString(GL.GL_EXTENSIONS); 
      if (-1 < ext.indexOf("texture_non_power_of_two")) {
        fboWidth = pg.width;
        fboHeight = pg.height;
      } else {
        fboWidth = PGL.nextPowerOfTwo(pg.width);
        fboHeight = PGL.nextPowerOfTwo(pg.height);
      }   
      multisample = 1 < numSamples;
      if (multisample && gl2x == null) {
        // We could add additional code to handle the lack of the packed depth+stencil extension, later... maybe.
        throw new RuntimeException("Doesn't have the OpenGL extensions necessary for multisampling."); 
      }      
      packedDepthStencil = ext.indexOf("packed_depth_stencil") != -1;
      
      contextHashCode = context.hashCode();
      
      // Create the color texture...
      gl.glGenTextures(1, glColorTexID, 0);
      gl.glBindTexture(GL.GL_TEXTURE_2D, glColorTexID[0]);    
      gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_NEAREST);    
      gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_NEAREST);
      gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S, GL.GL_CLAMP_TO_EDGE);
      gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T, GL.GL_CLAMP_TO_EDGE);
      gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, GL.GL_RGBA, fboWidth, fboHeight, 0, GL.GL_RGBA, GL.GL_UNSIGNED_BYTE, null);
      gl.glBindTexture(GL.GL_TEXTURE_2D, 0);      
     
      // ...and attach to the color framebuffer.
      gl.glGenFramebuffers(1, glColorFboID, 0); 
      gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, glColorFboID[0]);
      gl.glFramebufferTexture2D(GL.GL_FRAMEBUFFER, GL.GL_COLOR_ATTACHMENT0, GL.GL_TEXTURE_2D, glColorTexID[0], 0);
      
      // Clear the color buffer in the color FBO
      gl.glClearColor(0, 0, 0, 0);
      gl.glClear(GL.GL_COLOR_BUFFER_BIT);      
      
      if (multisample) {
        // We need multisampled FBO:
        
        gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, 0);
      
        // Now, creating mutisampled FBO with packed depth and stencil buffers.      
        gl.glGenFramebuffers(1, glMultiFboID, 0);
        gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, glMultiFboID[0]);
      
        // color render buffer...
        gl.glGenRenderbuffers(1, glColorRenderBufferID, 0);
        gl.glBindRenderbuffer(GL.GL_RENDERBUFFER, glColorRenderBufferID[0]);      
        gl2x.glRenderbufferStorageMultisample(GL.GL_RENDERBUFFER, numSamples, GL.GL_RGBA8, fboWidth, fboHeight);
        gl.glFramebufferRenderbuffer(GL.GL_FRAMEBUFFER, GL.GL_COLOR_ATTACHMENT0, GL.GL_RENDERBUFFER, glColorRenderBufferID[0]);
      
        if (packedDepthStencil) { 
          // packed depth+stencil buffer...
          gl.glGenRenderbuffers(1, glPackedDepthStencilID, 0);
          gl.glBindRenderbuffer(GL.GL_RENDERBUFFER, glPackedDepthStencilID[0]);      
          gl2x.glRenderbufferStorageMultisample(GL.GL_RENDERBUFFER, numSamples, GL.GL_DEPTH24_STENCIL8, fboWidth, fboHeight);
          gl.glFramebufferRenderbuffer(GL.GL_FRAMEBUFFER, GL.GL_DEPTH_ATTACHMENT, GL.GL_RENDERBUFFER, glPackedDepthStencilID[0]);
          gl.glFramebufferRenderbuffer(GL.GL_FRAMEBUFFER, GL.GL_STENCIL_ATTACHMENT, GL.GL_RENDERBUFFER, glPackedDepthStencilID[0]);
        } else {
          // Separate depth and stencil buffers...
          gl.glGenRenderbuffers(1, glDepthBufferID, 0);
          gl.glBindRenderbuffer(GL.GL_RENDERBUFFER, glDepthBufferID[0]);
          gl2x.glRenderbufferStorageMultisample(GL.GL_RENDERBUFFER, numSamples, GL.GL_DEPTH_COMPONENT24, fboWidth, fboHeight);          
          gl.glFramebufferRenderbuffer(GL.GL_FRAMEBUFFER, GL.GL_DEPTH_ATTACHMENT, GL.GL_RENDERBUFFER, glDepthBufferID[0]);
                    
          // TODO: separate depth buffer doesn't work, either in multisampled or single sample setups
//          gl.glGenRenderbuffers(1, glStencilBufferID, 0);
//          gl.glBindRenderbuffer(GL.GL_RENDERBUFFER, glStencilBufferID[0]);
//          gl2x.glRenderbufferStorageMultisample(GL.GL_RENDERBUFFER, numSamples, GL.GL_STENCIL_INDEX8, fboWidth, fboHeight);          
//          gl.glFramebufferRenderbuffer(GL.GL_FRAMEBUFFER, GL.GL_STENCIL_ATTACHMENT, GL.GL_RENDERBUFFER, glStencilBufferID[0]);
        }
            
        // Clear all the buffers in the multisample FBO
        gl.glClearDepth(1);
        gl.glClearStencil(0);
        gl.glClearColor(0, 0, 0, 0);
        gl.glClear(GL.GL_DEPTH_BUFFER_BIT | GL.GL_STENCIL_BUFFER_BIT | GL.GL_COLOR_BUFFER_BIT);
        
        // All set with multisampled FBO!
        gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, glColorFboID[0]);
      } else {
        if (packedDepthStencil) { 
          // packed depth+stencil buffer...
          gl.glGenRenderbuffers(1, glPackedDepthStencilID, 0);
          gl.glBindRenderbuffer(GL.GL_RENDERBUFFER, glPackedDepthStencilID[0]);      
          gl.glRenderbufferStorage(GL.GL_RENDERBUFFER, GL.GL_DEPTH24_STENCIL8, fboWidth, fboHeight);
          gl.glFramebufferRenderbuffer(GL.GL_FRAMEBUFFER, GL.GL_DEPTH_ATTACHMENT, GL.GL_RENDERBUFFER, glPackedDepthStencilID[0]);
          gl.glFramebufferRenderbuffer(GL.GL_FRAMEBUFFER, GL.GL_STENCIL_ATTACHMENT, GL.GL_RENDERBUFFER, glPackedDepthStencilID[0]);
        } else {
          // Separate depth and stencil buffers...
          gl.glGenRenderbuffers(1, glDepthBufferID, 0);
          gl.glBindRenderbuffer(GL.GL_RENDERBUFFER, glDepthBufferID[0]);
          gl.glRenderbufferStorage(GL.GL_RENDERBUFFER, GL.GL_DEPTH_COMPONENT24, fboWidth, fboHeight);          
          gl.glFramebufferRenderbuffer(GL.GL_FRAMEBUFFER, GL.GL_DEPTH_ATTACHMENT, GL.GL_RENDERBUFFER, glDepthBufferID[0]);
                    
          // TODO: separate depth buffer doesn't work, either in multisampled or single sample setups
//          gl.glGenRenderbuffers(1, glStencilBufferID, 0);
//          gl.glBindRenderbuffer(GL.GL_RENDERBUFFER, glStencilBufferID[0]);
//          gl.glRenderbufferStorage(GL.GL_RENDERBUFFER, GL.GL_STENCIL_INDEX8, fboWidth, fboHeight);          
//          gl.glFramebufferRenderbuffer(GL.GL_FRAMEBUFFER, GL.GL_STENCIL_ATTACHMENT, GL.GL_RENDERBUFFER, glStencilBufferID[0]);
        }
        
        // Clear all the buffers in the color FBO
        gl.glClearDepth(1);
        gl.glClearStencil(0);
        gl.glClear(GL.GL_DEPTH_BUFFER_BIT | GL.GL_STENCIL_BUFFER_BIT);    
      }      

      // The screen framebuffer is the color FBO just created. We need
      // to update the screenFramebuffer object so when the framebuffer 
      // is popped back to the screen, the correct id is set.      
      PGraphicsOpenGL.screenFramebuffer.glFbo = glColorFboID[0];      
    } else {
      // To make sure that the default screen buffer is used, specially after
      // doing screen rendering on an FBO (the OSX 10.7+ above).
      PGraphicsOpenGL.screenFramebuffer.glFbo = 0;
    }
  }


  public void updateOffscreen(PGL primary) {
    gl  = primary.gl;
    gl2 = primary.gl2;
    gl2x = primary.gl2x;
  }

  
  public boolean primaryIsDoubleBuffered() {
    // When using the multisampled FBO, the color
    // FBO is single buffered as it has only one
    // texture bound to it.
    return glColorFboID[0] == 0;
  }
  
  
  public boolean primaryIsFboBacked() {
    return glColorFboID[0] != 0;
  }
  

  public int getFboTexTarget() {
    return GL.GL_TEXTURE_2D;
   }  
  
  
  public int getFboTexName() {
    return glColorTexID[0];
   }
  
  
  public int getFboWidth() {
   return fboWidth;
  }

  
  public int getFboHeight() {
    return fboHeight;
   } 

  
  public void bindPrimaryColorFBO() {
    if (multisample) {
      // Blit the contents of the multisampled FBO into the color FBO,
      // so the later is up to date.
      gl.glBindFramebuffer(GL2.GL_READ_FRAMEBUFFER, glMultiFboID[0]);
      gl.glBindFramebuffer(GL2.GL_DRAW_FRAMEBUFFER, glColorFboID[0]);
      gl2x.glBlitFramebuffer(0, 0, fboWidth, fboHeight,
                             0, 0, fboWidth, fboHeight, 
                             GL.GL_COLOR_BUFFER_BIT, GL.GL_NEAREST);
    }
    
    gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, glColorFboID[0]);
    PGraphicsOpenGL.screenFramebuffer.glFbo = glColorFboID[0];
    
    // Make the color buffer opaque so it doesn't show      
    // the background when drawn on top of another surface. 
    gl.glColorMask(false, false, false, true);
    gl.glClearColor(0, 0, 0, 1);
    gl.glClear(GL.GL_COLOR_BUFFER_BIT);
    gl.glColorMask(true, true, true, true);
  }

  
  public void bindPrimaryMultiFBO() {
    if (multisample) {
      gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, glMultiFboID[0]);   
      PGraphicsOpenGL.screenFramebuffer.glFbo = glMultiFboID[0];
    }
  }
  
  
  protected void releaseScreenFBO() {
    gl.glDeleteTextures(1, glColorTexID, 0);
    gl.glDeleteFramebuffers(1, glColorFboID, 0);
    if (packedDepthStencil) {
      gl.glDeleteRenderbuffers(1, glPackedDepthStencilID, 0);
    } else {
      gl.glDeleteRenderbuffers(1, glDepthBufferID, 0);
      gl.glDeleteRenderbuffers(1, glStencilBufferID, 0);
    }
    if (multisample) {
      gl.glDeleteFramebuffers(1, glMultiFboID, 0);
      gl.glDeleteRenderbuffers(1, glColorRenderBufferID, 0);
    }    
  }
  
  
  protected int qualityToSamples(int quality) {
    if (quality <= 1) {
      return 1;
    } else {
      // Number of samples is always an even number:
      int n = 2 * (quality / 2);
      return n;
    }
  }

  
  ///////////////////////////////////////////////////////////////////////////////////

  // Frame rendering


  public void beginOnscreenDraw(boolean clear) {
    if (glColorFboID[0] != 0) {
      if (multisample) {
        // Render the scene to the mutisampled buffer...
        gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, glMultiFboID[0]);    
        gl2x.glDrawBuffer(GL.GL_COLOR_ATTACHMENT0);
        
        // Now the screen buffer is the multisample FBO.
        PGraphicsOpenGL.screenFramebuffer.glFbo = glMultiFboID[0];
      } else {
        gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, glColorFboID[0]);
        if (gl2x != null) gl2x.glDrawBuffer(GL.GL_COLOR_ATTACHMENT0);
        
        PGraphicsOpenGL.screenFramebuffer.glFbo = glColorFboID[0];
      }
    }
  }


  public void endOnscreenDraw(boolean clear0) {
    if (glColorFboID[0] != 0) {
      if (multisample) {
        // Blit the contents of the multisampled FBO into the color FBO:
        gl.glBindFramebuffer(GL2.GL_READ_FRAMEBUFFER, glMultiFboID[0]);
        gl.glBindFramebuffer(GL2.GL_DRAW_FRAMEBUFFER, glColorFboID[0]);
        gl2x.glBlitFramebuffer(0, 0, fboWidth, fboHeight,
                               0, 0, fboWidth, fboHeight, 
                               GL.GL_COLOR_BUFFER_BIT, GL.GL_NEAREST);
      }
      
      // And finally write the color texture to the screen, without blending.
      gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, 0);
            
      gl.glClearDepth(1);
      gl.glClearColor(0, 0, 0, 0);
      gl.glClear(GL.GL_DEPTH_BUFFER_BIT | GL.GL_STENCIL_BUFFER_BIT);
      
      gl.glDisable(GL.GL_BLEND);
      drawTexture(GL.GL_TEXTURE_2D, glColorTexID[0], fboWidth, fboHeight, 0, 0, pg.width, pg.height, 0, 0, pg.width, pg.height);

      // Leaving the color FBO currently bound as the screen FB.
      gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, glColorFboID[0]);
      PGraphicsOpenGL.screenFramebuffer.glFbo = glColorFboID[0];       
    }
  }


  public void beginOffscreenDraw(boolean clear) {
  }


  public void endOffscreenDraw(boolean clear0) {
  }


  public boolean canDraw() {
    return initialized && pg.parent.isDisplayable();
  }


  public void requestDraw() {
    if (initialized) {
      //animator.requestDisplay();
      
      if (toolkit == AWT) {
        canvasAWT.display();
      } else if (toolkit == NEWT) {
        animator.requestDisplay();
      }
      
    }
  }


  ///////////////////////////////////////////////////////////////////////////////////

  // Caps query


  public String glGetString(int name) {
    return gl.glGetString(name);
  }


  public void glGetIntegerv(int name, int[] values, int offset) {
    gl.glGetIntegerv(name, values, offset);
  }


  public void glGetBooleanv(int name, boolean[] values, int offset) {
    if (-1 < name) {
      byte[] bvalues = new byte[values.length];
      gl.glGetBooleanv(name, bvalues, offset);
      for (int i = 0; i < values.length; i++) {
        values[i] = bvalues[i] != 0;
      }
    } else {
      Arrays.fill(values, false);
    }
  }


  ///////////////////////////////////////////////////////////////////////////////////

  // Enable/disable caps


  public void glEnable(int cap) {
    if (-1 < cap) {
      gl.glEnable(cap);
    }
  }


  public void glDisable(int cap) {
    if (-1 < cap) {
      gl.glDisable(cap);
    }
  }


  ///////////////////////////////////////////////////////////////////////////////////

  // Render control


  public void glFlush() {
    gl.glFlush();
  }


  public void glFinish() {
    gl.glFinish();
  }


  /////////////////////////////////////////////////////////////////////////////////

  // Error handling


  public int glGetError() {
    return gl.glGetError();
  }


  public String glErrorString(int err) {
    return glu.gluErrorString(err);
  }


  public String gluErrorString(int err) {
    return glu.gluErrorString(err);
  }


  /////////////////////////////////////////////////////////////////////////////////

  // Rendering options


  public void glFrontFace(int mode) {
    gl.glFrontFace(mode);
  }

  
  public void glCullFace(int mode) {
    gl.glCullFace(mode);
  }
  

  public void glDepthMask(boolean flag) {
    gl.glDepthMask(flag);
  }


  public void glDepthFunc(int func) {
    gl.glDepthFunc(func);
  }


  /////////////////////////////////////////////////////////////////////////////////

  // Textures


  public void glGenTextures(int n, int[] ids, int offset) {
    gl.glGenTextures(n, ids, offset);
  }


  public void glDeleteTextures(int n, int[] ids, int offset) {
    gl.glDeleteTextures(n, ids, offset);
  }


  public void glActiveTexture(int unit) {
    gl.glActiveTexture(unit);
  }


  public void glBindTexture(int target, int id) {
    gl.glBindTexture(target, id);
  }


  public void glTexImage2D(int target, int level, int internalFormat, int width, int height, int border, int format, int type, Buffer data) {
    gl.glTexImage2D(target, level, internalFormat, width, height, border, format, type, data);
  }


  public void glTexSubImage2D(int target, int level, int xOffset, int yOffset, int width, int height, int format, int type, Buffer data) {
    gl.glTexSubImage2D(target, level, xOffset, yOffset, width, height, format, type, data);
  }


  public void glTexParameteri(int target, int param, int value) {
    gl.glTexParameteri(target, param, value);
  }

  
  public void glGetTexParameteriv(int target, int param, int[] values, int offset) {
    gl.glGetTexParameteriv(target, param, values, offset);
  }
  

  public void glGenerateMipmap(int target) {
    gl.glGenerateMipmap(target);
  }


  /////////////////////////////////////////////////////////////////////////////////

  // Vertex Buffers


  public void glGenBuffers(int n, int[] ids, int offset) {
    gl.glGenBuffers(n, ids, offset);
  }


  public void glDeleteBuffers(int n, int[] ids, int offset) {
    gl.glDeleteBuffers(n, ids, offset);
  }


  public void glBindBuffer(int target, int id) {
    gl.glBindBuffer(target, id);
  }


  public void glBufferData(int target, int size, Buffer data, int usage) {
    gl.glBufferData(target, size, data, usage);
  }


  public void glBufferSubData(int target, int offset, int size, Buffer data) {
    gl.glBufferSubData(target, offset, size, data);
  }


  public void glDrawArrays(int mode, int first, int count) {
    gl.glDrawArrays(mode, first, count);
  }


  public void glDrawElements(int mode, int count, int type, int offset) {
    gl.glDrawElements(mode, count, type, offset);
  }


  public void glEnableVertexAttribArray(int loc) {
    gl2.glEnableVertexAttribArray(loc);
  }


  public void glDisableVertexAttribArray(int loc) {
    gl2.glDisableVertexAttribArray(loc);
  }


  public void glVertexAttribPointer(int loc, int size, int type, boolean normalized, int stride, int offset) {
    gl2.glVertexAttribPointer(loc, size, type, normalized, stride, offset);
  }


  public void glVertexAttribPointer(int loc, int size, int type, boolean normalized, int stride, Buffer data) {
    gl2.glVertexAttribPointer(loc, size, type, normalized, stride, data);
  }


  public ByteBuffer glMapBuffer(int target, int access) {
    return gl2.glMapBuffer(target, access);
  }


  public ByteBuffer glMapBufferRange(int target, int offset, int length, int access) {
    if (gl2x != null) {
      return gl2x.glMapBufferRange(target, offset, length, access);
    } else {
      return null;
    }
  }


  public void glUnmapBuffer(int target) {
    gl2.glUnmapBuffer(target);
  }


  /////////////////////////////////////////////////////////////////////////////////

  // Framebuffers, renderbuffers


  public void glGenFramebuffers(int n, int[] ids, int offset) {
    gl.glGenFramebuffers(n, ids, offset);
  }


  public void glDeleteFramebuffers(int n, int[] ids, int offset) {
    gl.glDeleteFramebuffers(n, ids, offset);
  }


  public void glGenRenderbuffers(int n, int[] ids, int offset) {
    gl.glGenRenderbuffers(n, ids, offset);
  }


  public void glDeleteRenderbuffers(int n, int[] ids, int offset) {
    gl.glDeleteRenderbuffers(n, ids, offset);
  }


  public void glBindFramebuffer(int target, int id) {
    gl.glBindFramebuffer(target, id);
  }


  public void glBlitFramebuffer(int srcX0, int srcY0, int srcX1, int srcY1, int dstX0, int dstY0, int dstX1, int dstY1, int mask, int filter) {
    if (gl2x != null) {
      gl2x.glBlitFramebuffer(srcX0, srcY0, srcX1, srcY1, dstX0, dstY0, dstX1, dstY1, mask, filter);
    }
  }


  public void glFramebufferTexture2D(int target, int attachment, int texTarget, int texId, int level) {
    gl.glFramebufferTexture2D(target, attachment, texTarget, texId, level);
  }


  public void glBindRenderbuffer(int target, int id) {
    gl.glBindRenderbuffer(target, id);
  }


  public void glRenderbufferStorageMultisample(int target, int samples, int format, int width, int height) {
    if (gl2x != null) {
      gl2x.glRenderbufferStorageMultisample(target, samples, format, width, height);
    }
  }


  public void glRenderbufferStorage(int target, int format, int width, int height) {
    gl.glRenderbufferStorage(target, format, width, height);
  }


  public void glFramebufferRenderbuffer(int target, int attachment, int rendbufTarget, int rendbufId) {
    gl.glFramebufferRenderbuffer(target, attachment, rendbufTarget, rendbufId);
  }


  public int glCheckFramebufferStatus(int target) {
    return gl.glCheckFramebufferStatus(target);
  }


  /////////////////////////////////////////////////////////////////////////////////

  // Shaders


  public int glCreateProgram() {
    return gl2.glCreateProgram();
  }


  public void glDeleteProgram(int id) {
    gl2.glDeleteProgram(id);
  }


  public int glCreateShader(int type) {
    return gl2.glCreateShader(type);
  }


  public void glDeleteShader(int id) {
    gl2.glDeleteShader(id);
  }


  public void glLinkProgram(int prog) {
    gl2.glLinkProgram(prog);
  }


  public void glValidateProgram(int prog) {
    gl2.glValidateProgram(prog);
  }


  public void glUseProgram(int prog) {
    gl2.glUseProgram(prog);
  }


  public int glGetAttribLocation(int prog, String name) {
    return gl2.glGetAttribLocation(prog, name);
  }


  public int glGetUniformLocation(int prog, String name) {
    return gl2.glGetUniformLocation(prog, name);
  }


  public void glUniform1i(int loc, int value) {
    gl2.glUniform1i(loc, value);
  }


  public void glUniform2i(int loc, int value0, int value1) {
    gl2.glUniform2i(loc, value0, value1);
  }  

  
  public void glUniform3i(int loc, int value0, int value1, int value2) {
    gl2.glUniform3i(loc, value0, value1, value2);
  }    
  

  public void glUniform4i(int loc, int value0, int value1, int value2, int value3) {
    gl2.glUniform4i(loc, value0, value1, value2, value3);
  }      
  
  
  public void glUniform1f(int loc, float value) {
    gl2.glUniform1f(loc, value);
  }


  public void glUniform2f(int loc, float value0, float value1) {
    gl2.glUniform2f(loc, value0, value1);
  }


  public void glUniform3f(int loc, float value0, float value1, float value2) {
    gl2.glUniform3f(loc, value0, value1, value2);
  }


  public void glUniform4f(int loc, float value0, float value1, float value2, float value3) {
    gl2.glUniform4f(loc, value0, value1, value2, value3);
  }


  public void glUniform1iv(int loc, int count, int[] v, int offset) {
    gl2.glUniform1iv(loc, count, v, offset);
  }
  
  
  public void glUniform2iv(int loc, int count, int[] v, int offset) {
    gl2.glUniform2iv(loc, count, v, offset);
  }
  

  public void glUniform3iv(int loc, int count, int[] v, int offset) {
    gl2.glUniform3iv(loc, count, v, offset);
  }
  

  public void glUniform4iv(int loc, int count, int[] v, int offset) {
    gl2.glUniform4iv(loc, count, v, offset);
  }  
  
  
  public void glUniform1fv(int loc, int count, float[] v, int offset) {
    gl2.glUniform1fv(loc, count, v, offset);
  }


  public void glUniform2fv(int loc, int count, float[] v, int offset) {
    gl2.glUniform2fv(loc, count, v, offset);
  }


  public void glUniform3fv(int loc, int count, float[] v, int offset) {
    gl2.glUniform3fv(loc, count, v, offset);
  }


  public void glUniform4fv(int loc, int count, float[] v, int offset) {
    gl2.glUniform4fv(loc, count, v, offset);
  }


  public void glUniformMatrix2fv(int loc, int count, boolean transpose, float[] mat, int offset) {
    gl2.glUniformMatrix2fv(loc, count, transpose, mat, offset);
  }


  public void glUniformMatrix3fv(int loc, int count, boolean transpose, float[] mat, int offset) {
    gl2.glUniformMatrix3fv(loc, count, transpose, mat, offset);
  }


  public void glUniformMatrix4fv(int loc, int count, boolean transpose, float[] mat, int offset) {
    gl2.glUniformMatrix4fv(loc, count, transpose, mat, offset);
  }


  public void glVertexAttrib1f(int loc, float value) {
    gl2.glVertexAttrib1f(loc, value);
  }


  public void glVertexAttrib2f(int loc, float value0, float value1) {
    gl2.glVertexAttrib2f(loc, value0, value1);
  }


  public void glVertexAttrib3f(int loc, float value0, float value1, float value2) {
    gl2.glVertexAttrib3f(loc, value0, value1, value2);
  }


  public void glVertexAttrib4f(int loc, float value0, float value1, float value2, float value3) {
    gl2.glVertexAttrib4f(loc, value0, value1, value2, value3); 
  }


  public void glVertexAttrib1fv(int loc, float[] v, int offset) {
    gl2.glVertexAttrib1fv(loc, v, offset);  
  }

  
  public void glVertexAttrib2fv(int loc, float[] v, int offset) {
    gl2.glVertexAttrib2fv(loc, v, offset);  
  }


  public void glVertexAttrib3fv(int loc, float[] v, int offset) {
    gl2.glVertexAttrib3fv(loc, v, offset);  
  }
 

  public void glVertexAttrib4fv(int loc, float[] v, int offset) {
    gl2.glVertexAttrib4fv(loc, v, offset);  
  }  
  

  public void glShaderSource(int id, String source) {
    gl2.glShaderSource(id, 1, new String[] { source }, (int[]) null, 0);
  }


  public void glCompileShader(int id) {
    gl2.glCompileShader(id);
  }


  public void glAttachShader(int prog, int shader) {
    gl2.glAttachShader(prog, shader);
  }


  public void glGetShaderiv(int shader, int pname, int[] params, int offset) {
    gl2.glGetShaderiv(shader, pname, params, offset);
  }


  public String glGetShaderInfoLog(int shader) {
    int[] val = { 0 };
    gl2.glGetShaderiv(shader, GL2.GL_INFO_LOG_LENGTH, val, 0);
    int length = val[0];

    byte[] log = new byte[length];
    gl2.glGetShaderInfoLog(shader, length, val, 0, log, 0);
    return new String(log);
  }


  public void glGetProgramiv(int prog, int pname, int[] params, int offset) {
    gl2.glGetProgramiv(prog, pname, params, offset);
  }


  public String glGetProgramInfoLog(int prog) {
    int[] val = { 0 };
    gl2.glGetShaderiv(prog, GL2.GL_INFO_LOG_LENGTH, val, 0);
    int length = val[0];

    byte[] log = new byte[length];
    gl2.glGetProgramInfoLog(prog, length, val, 0, log, 0);
    return new String(log);
  }


  /////////////////////////////////////////////////////////////////////////////////

  // Viewport


  public void glViewport(int x, int y, int width, int height) {
    gl.glViewport(x, y, width, height);
  }


  /////////////////////////////////////////////////////////////////////////////////

  // Clipping (scissor test)


  public void glScissor(int x, int y, int w, int h) {
    gl.glScissor(x, y, w, h);
  }


  /////////////////////////////////////////////////////////////////////////////////

  // Blending


  public void glBlendEquation(int eq) {
    gl.glBlendEquation(eq);
  }


  public void glBlendFunc(int srcFactor, int dstFactor) {
    gl.glBlendFunc(srcFactor, dstFactor);
  }


  /////////////////////////////////////////////////////////////////////////////////

  // Pixels


  public void glReadBuffer(int buf) {
    if (gl2x != null) {
      gl2x.glReadBuffer(buf);
    }
  }


  public void glReadPixels(int x, int y, int width, int height, int format, int type, Buffer buffer) {
    gl.glReadPixels(x, y, width, height, format, type, buffer);
  }


  public void glDrawBuffer(int buf) {
    if (gl2x != null) {
      gl2x.glDrawBuffer(buf);
    }
  }

  
  public void glClearDepth(float d) {
    gl.glClearDepthf(d);
  }  

  
  public void glClearStencil(int s) {
    gl.glClearStencil(s);
  }

  
  public void glColorMask(boolean wr, boolean wg, boolean wb, boolean wa) {
    gl.glColorMask(wr, wg, wb, wa);
  }
  

  public void glClearColor(float r, float g, float b, float a) {
    gl.glClearColor(r, g, b, a);
  }


  public void glClear(int mask) {
    gl.glClear(mask);
  }


  /////////////////////////////////////////////////////////////////////////////////

  // Context interface

  
  public Context createEmptyContext() {
    return new Context();
  }
  

  public Context getCurrentContext() {
    return new Context(context);
  }

  
  public class Context {
    protected GLContext glContext;

    Context() {
      glContext = null;    
    }
    
    Context(GLContext context) {
      glContext = context;
    }

    boolean current() {
      return equal(context);
    }    
    
    boolean equal(GLContext context) {
      if (glContext == null || context == null) {
        // A null context means a still non-created resource,
        // so it is considered equal to the argument.
        return true; 
      } else {        
        return glContext.hashCode() == context.hashCode();
      }
    }
    
    int code() {
      if (glContext == null) {
        return -1;
      } else {
        return glContext.hashCode();
      }
    }
  }


  /////////////////////////////////////////////////////////////////////////////////

  // Tessellator interface


  public Tessellator createTessellator(TessellatorCallback callback) {
    return new Tessellator(callback);
  }


  public class Tessellator {
    protected GLUtessellator tess;
    protected TessellatorCallback callback;
    protected GLUCallback gluCallback;

    public Tessellator(TessellatorCallback callback) {
      this.callback = callback;
      tess = GLU.gluNewTess();
      gluCallback = new GLUCallback();

      GLU.gluTessCallback(tess, GLU.GLU_TESS_BEGIN, gluCallback);
      GLU.gluTessCallback(tess, GLU.GLU_TESS_END, gluCallback);
      GLU.gluTessCallback(tess, GLU.GLU_TESS_VERTEX, gluCallback);
      GLU.gluTessCallback(tess, GLU.GLU_TESS_COMBINE, gluCallback);
      GLU.gluTessCallback(tess, GLU.GLU_TESS_ERROR, gluCallback);
    }

    public void beginPolygon() {
      GLU.gluTessBeginPolygon(tess, null);
    }

    public void endPolygon() {
      GLU.gluTessEndPolygon(tess);
    }

    public void setWindingRule(int rule) {
      GLU.gluTessProperty(tess, GLU.GLU_TESS_WINDING_RULE, rule);
    }

    public void beginContour() {
      GLU.gluTessBeginContour(tess);
    }

    public void endContour() {
      GLU.gluTessEndContour(tess);
    }

    public void addVertex(double[] v) {
      GLU.gluTessVertex(tess, v, 0, v);
    }

    protected class GLUCallback extends GLUtessellatorCallbackAdapter {
      public void begin(int type) {
        callback.begin(type);
      }

      public void end() {
        callback.end();
      }

      public void vertex(Object data) {
        callback.vertex(data);
      }

      public void combine(double[] coords, Object[] data,
                          float[] weight, Object[] outData) {
        callback.combine(coords, data, weight, outData);
      }

      public void error(int errnum) {
        callback.error(errnum);
      }
    }
  }


  public interface TessellatorCallback  {
    public void begin(int type);
    public void end();
    public void vertex(Object data);
    public void combine(double[] coords, Object[] data,
                        float[] weight, Object[] outData);
    public void error(int errnum);
  }


  ///////////////////////////////////////////////////////////////////////////////////

  // Utility functions


  public boolean contextIsCurrent(Context other) {
    return other == null || other.current();
  }


  public void enableTexturing(int target) {
    glEnable(target);
  }


  public void disableTexturing(int target) {
    glDisable(target);
  }


  public void initTexture(int target, int format, int width, int height) {
    int[] texels = new int[width * height];
    glTexSubImage2D(target, 0, 0, 0, width, height, format, GL_UNSIGNED_BYTE, IntBuffer.wrap(texels));
  }


  public void copyToTexture(int target, int format, int id, int x, int y, int w, int h, IntBuffer buffer) {
    enableTexturing(target);
    glBindTexture(target, id);
    glTexSubImage2D(target, 0, x, y, w, h, format, GL_UNSIGNED_BYTE, buffer);
    glBindTexture(target, 0);
    disableTexturing(target);
  }


  public void drawTexture(int target, int id, int width, int height,
                          int X0, int Y0, int X1, int Y1) {
    drawTexture(target, id, width, height, X0, Y0, X1, Y1, X0, Y0, X1, Y1);
  }

  public void drawTexture(int target, int id, int width, int height,
                          int texX0, int texY0, int texX1, int texY1,
                          int scrX0, int scrY0, int scrX1, int scrY1) {
    if (target == GL_TEXTURE_2D) {
      drawTexture2D(id, width, height,
                    texX0, texY0, texX1, texY1,
                    scrX0, scrY0, scrX1, scrY1);
    } else if (target == GL_TEXTURE_RECTANGLE) {
      drawTextureRect(id, width, height,
                      texX0, texY0, texX1, texY1,
                      scrX0, scrY0, scrX1, scrY1);      
    }
  }
  
  public void drawTexture2D(int id, int width, int height,
                            int texX0, int texY0, int texX1, int texY1,
                            int scrX0, int scrY0, int scrX1, int scrY1) {
    if (!loadedTex2DShader || tex2DShaderContext.hashCode() != context.hashCode()) {
      tex2DVertShader = createShader(GL_VERTEX_SHADER, texVertShaderSource);
      tex2DFragShader = createShader(GL_FRAGMENT_SHADER, tex2DFragShaderSource);
      if (0 < tex2DVertShader && 0 < tex2DFragShader) {
        tex2DShaderProgram = createProgram(tex2DVertShader, tex2DFragShader);
      }
      if (0 < tex2DShaderProgram) {
        tex2DVertLoc = glGetAttribLocation(tex2DShaderProgram, "inVertex");
        tex2DTCoordLoc = glGetAttribLocation(tex2DShaderProgram, "inTexcoord");
      }      
      loadedTex2DShader = true;
      tex2DShaderContext = context;
    }
    
    if (texData == null) {
      texData = allocateDirectFloatBuffer(texCoords.length);
    }
    
    if (0 < tex2DShaderProgram) {
      // The texture overwrites anything drawn earlier.
      boolean[] depthTest = new boolean[1];
      glGetBooleanv(GL_DEPTH_TEST, depthTest, 0);
      glDisable(GL_DEPTH_TEST);
      
      // When drawing the texture we don't write to the
      // depth mask, so the texture remains in the background
      // and can be occluded by anything drawn later, even if
      // if it is behind it.
      boolean[] depthMask = new boolean[1];
      glGetBooleanv(GL_DEPTH_WRITEMASK, depthMask, 0);
      glDepthMask(false);
      
      glUseProgram(tex2DShaderProgram);
      
      glEnableVertexAttribArray(tex2DVertLoc);
      glEnableVertexAttribArray(tex2DTCoordLoc);

      // Vertex coordinates of the textured quad are specified
      // in normalized screen space (-1, 1):
      // Corner 1
      texCoords[ 0] = 2 * (float)scrX0 / pg.width - 1;
      texCoords[ 1] = 2 * (float)scrY0 / pg.height - 1;
      texCoords[ 2] = (float)texX0 / width;
      texCoords[ 3] = (float)texY0 / height;      
      // Corner 2
      texCoords[ 4] = 2 * (float)scrX1 / pg.width - 1;
      texCoords[ 5] = 2 * (float)scrY0 / pg.height - 1; 
      texCoords[ 6] = (float)texX1 / width;
      texCoords[ 7] = (float)texY0 / height;      
      // Corner 3
      texCoords[ 8] = 2 * (float)scrX0 / pg.width - 1;
      texCoords[ 9] = 2 * (float)scrY1 / pg.height - 1;      
      texCoords[10] = (float)texX0 / width;
      texCoords[11] = (float)texY1 / height;      
      // Corner 4
      texCoords[12] = 2 * (float)scrX1 / pg.width - 1;
      texCoords[13] = 2 * (float)scrY1 / pg.height - 1;
      texCoords[14] = (float)texX1 / width;
      texCoords[15] = (float)texY1 / height;        

      texData.rewind();
      texData.put(texCoords);
      
      enableTexturing(GL_TEXTURE_2D);
      glActiveTexture(GL_TEXTURE0);
      glBindTexture(GL_TEXTURE_2D, id);
                
      glBindBuffer(GL_ARRAY_BUFFER, 0); // Making sure that no VBO is bound at this point.
      
      texData.position(0);
      glVertexAttribPointer(tex2DVertLoc, 2, GL_FLOAT, false, 4 * SIZEOF_FLOAT, texData);
      texData.position(2);
      glVertexAttribPointer(tex2DTCoordLoc, 2, GL_FLOAT, false, 4 * SIZEOF_FLOAT, texData);
      
      glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
      
      glBindTexture(GL_TEXTURE_2D, 0);
      disableTexturing(GL_TEXTURE_2D);

      glDisableVertexAttribArray(tex2DVertLoc);
      glDisableVertexAttribArray(tex2DTCoordLoc);

      glUseProgram(0);

      if (depthTest[0]) {
        glEnable(GL_DEPTH_TEST);
      } else {
        glDisable(GL_DEPTH_TEST);
      }      
      glDepthMask(depthMask[0]);
    }
  }
  
  
  public void drawTextureRect(int id, int width, int height,
                              int texX0, int texY0, int texX1, int texY1,
                              int scrX0, int scrY0, int scrX1, int scrY1) {
    if (!loadedTexRectShader || texRectShaderContext.hashCode() != context.hashCode()) {
      texRectVertShader = createShader(GL_VERTEX_SHADER, texVertShaderSource);
      texRectFragShader = createShader(GL_FRAGMENT_SHADER, texRectFragShaderSource);
      if (0 < texRectVertShader && 0 < texRectFragShader) {
        texRectShaderProgram = createProgram(texRectVertShader, texRectFragShader);
      }
      if (0 < texRectShaderProgram) {
        texRectVertLoc = glGetAttribLocation(texRectShaderProgram, "inVertex");
        texRectTCoordLoc = glGetAttribLocation(texRectShaderProgram, "inTexcoord");
      }      
      loadedTexRectShader = true;
      texRectShaderContext = context;
    }
    
    if (texData == null) {
      texData = allocateDirectFloatBuffer(texCoords.length);
    }
    
    if (0 < texRectShaderProgram) {
      // The texture overwrites anything drawn earlier.
      boolean[] depthTest = new boolean[1];
      glGetBooleanv(GL_DEPTH_TEST, depthTest, 0);
      glDisable(GL_DEPTH_TEST);
      
      // When drawing the texture we don't write to the
      // depth mask, so the texture remains in the background
      // and can be occluded by anything drawn later, even if
      // if it is behind it.
      boolean[] depthMask = new boolean[1];
      glGetBooleanv(GL_DEPTH_WRITEMASK, depthMask, 0);
      glDepthMask(false);
      
      glUseProgram(texRectShaderProgram);
      
      glEnableVertexAttribArray(texRectVertLoc);
      glEnableVertexAttribArray(texRectTCoordLoc);

      // Vertex coordinates of the textured quad are specified
      // in normalized screen space (-1, 1):
      // Corner 1
      texCoords[ 0] = 2 * (float)scrX0 / pg.width - 1;
      texCoords[ 1] = 2 * (float)scrY0 / pg.height - 1;
      texCoords[ 2] = texX0;
      texCoords[ 3] = texY0;      
      // Corner 2
      texCoords[ 4] = 2 * (float)scrX1 / pg.width - 1;
      texCoords[ 5] = 2 * (float)scrY0 / pg.height - 1;
      texCoords[ 6] = texX1;
      texCoords[ 7] = texY0;       
      // Corner 3
      texCoords[ 8] = 2 * (float)scrX0 / pg.width - 1;
      texCoords[ 9] = 2 * (float)scrY1 / pg.height - 1;    
      texCoords[10] = texX0;
      texCoords[11] = texY1;      
      // Corner 4
      texCoords[12] = 2 * (float)scrX1 / pg.width - 1;
      texCoords[13] = 2 * (float)scrY1 / pg.height - 1;
      texCoords[14] = texX1;
      texCoords[15] = texY1;       

      texData.rewind();
      texData.put(texCoords);
      
      enableTexturing(GL_TEXTURE_RECTANGLE);
      glActiveTexture(GL_TEXTURE0);
      glBindTexture(GL_TEXTURE_RECTANGLE, id);
                
      glBindBuffer(GL_ARRAY_BUFFER, 0); // Making sure that no VBO is bound at this point.
      
      texData.position(0);
      glVertexAttribPointer(texRectVertLoc, 2, GL_FLOAT, false, 4 * SIZEOF_FLOAT, texData);
      texData.position(2);
      glVertexAttribPointer(texRectTCoordLoc, 2, GL_FLOAT, false, 4 * SIZEOF_FLOAT, texData);
      
      glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
      
      glBindTexture(GL_TEXTURE_RECTANGLE, 0);
      disableTexturing(GL_TEXTURE_RECTANGLE);

      glDisableVertexAttribArray(texRectVertLoc);
      glDisableVertexAttribArray(texRectTCoordLoc);

      glUseProgram(0);

      if (depthTest[0]) {
        glEnable(GL_DEPTH_TEST);
      } else {
        glDisable(GL_DEPTH_TEST);
      }      
      glDepthMask(depthMask[0]);
    }
  }

  
  public void drawRectangle(float r, float g, float b, float a,
                            int scrX0, int scrY0, int scrX1, int scrY1) {
    if (!loadedRectShader || rectShaderContext.hashCode() != context.hashCode()) {
      rectVertShader = createShader(GL_VERTEX_SHADER, rectVertShaderSource);
      rectFragShader = createShader(GL_FRAGMENT_SHADER, rectFragShaderSource);
      if (0 < rectVertShader && 0 < rectFragShader) {
        rectShaderProgram = createProgram(rectVertShader, rectFragShader);
      }
      if (0 < rectShaderProgram) {
        rectVertLoc = glGetAttribLocation(rectShaderProgram, "inVertex");
        rectColorLoc = glGetUniformLocation(rectShaderProgram, "rectColor");
      }
      rectData = allocateDirectFloatBuffer(rectCoords.length);
      loadedRectShader = true;
      rectShaderContext = context;
    }

    if (0 < rectShaderProgram) {
      // The rectangle overwrites anything drawn earlier.
      boolean[] depthTest = new boolean[1];
      glGetBooleanv(GL_DEPTH_TEST, depthTest, 0);
      glDisable(GL_DEPTH_TEST);  
      
      // When drawing the rectangle we don't write to the
      // depth mask, so the rectangle remains in the background
      // and can be occluded by anything drawn later, even if
      // if it is behind it.
      boolean[] depthMask = new boolean[1];
      glGetBooleanv(GL_DEPTH_WRITEMASK, depthMask, 0);
      glDepthMask(false);

      glUseProgram(rectShaderProgram);

      glEnableVertexAttribArray(rectVertLoc);
      glUniform4f(rectColorLoc, r, g, b, a);

      // Vertex coordinates of the rectangle are specified
      // in normalized screen space (-1, 1):

      // Corner 1
      rectCoords[0] = 2 * (float)scrX0 / pg.width - 1;
      rectCoords[1] = 2 * (float)scrY0 / pg.height - 1;

      // Corner 2
      rectCoords[2] = 2 * (float)scrX1 / pg.width - 1;
      rectCoords[3] = 2 * (float)scrY0 / pg.height - 1;

      // Corner 3
      rectCoords[4] = 2 * (float)scrX0 / pg.width - 1;
      rectCoords[5] = 2 * (float)scrY1 / pg.height - 1;

      // Corner 4
      rectCoords[6] = 2 * (float)scrX1 / pg.width - 1;
      rectCoords[7] = 2 * (float)scrY1 / pg.height - 1;

      rectData.rewind();
      rectData.put(rectCoords);

      glBindBuffer(GL_ARRAY_BUFFER, 0); // Making sure that no VBO is bound at this point.
      
      rectData.position(0);
      glVertexAttribPointer(rectVertLoc, 2, GL_FLOAT, false, 2 * SIZEOF_FLOAT, rectData);

      glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);

      glDisableVertexAttribArray(rectVertLoc);

      glUseProgram(0);

      if (depthTest[0]) {
        glEnable(GL_DEPTH_TEST);
      } else {
        glDisable(GL_DEPTH_TEST);
      }      
      glDepthMask(depthMask[0]);
    }
  }


  public int getColorValue(int scrX, int scrY) {
    if (colorBuffer == null) {
      colorBuffer = IntBuffer.allocate(1);
    }
    colorBuffer.rewind();
    glReadPixels(scrX, pg.height - scrY - 1, 1, 1, GL_RGBA, GL_UNSIGNED_BYTE, colorBuffer);
    return colorBuffer.get();
  }


  public float getDepthValue(int scrX, int scrY) {
    if (depthBuffer == null) {
      depthBuffer = FloatBuffer.allocate(1);
    }
    depthBuffer.rewind();
    glReadPixels(scrX, pg.height - scrY - 1, 1, 1, GL_DEPTH_COMPONENT, GL_FLOAT, depthBuffer);
    return depthBuffer.get(0);
  }


  public byte getStencilValue(int scrX, int scrY) {
    if (stencilBuffer == null) {
      stencilBuffer = ByteBuffer.allocate(1);
    }
    glReadPixels(scrX, pg.height - scrY - 1, 1, 1, GL_STENCIL_INDEX, GL.GL_UNSIGNED_BYTE, stencilBuffer);
    return stencilBuffer.get(0);
  }


  // bit shifting this might be more efficient
  public static int nextPowerOfTwo(int val) {
    int ret = 1;
    while (ret < val) {
      ret <<= 1;
    }
    return ret;
  }
  
  
  /**
   * Converts input native OpenGL value (RGBA on big endian, ABGR on little 
   * endian) to Java ARGB.
   */  
  public static int nativeToJavaARGB(int color) {
    if (BIG_ENDIAN) { // RGBA to ARGB
      return (color & 0xff000000) |
             ((color >> 8) & 0x00ffffff);
    } else { // ABGR to ARGB
      return (color & 0xff000000) |
             ((color << 16) & 0xff0000) |
             (color & 0xff00) |
             ((color >> 16) & 0xff);
    }
  }  
  
  
  /**
   * Converts input array of native OpenGL values (RGBA on big endian, ABGR on little
   * endian) representing an image of width x height resolution to Java ARGB.
   * It also rearranges the elements in the array so that the image is flipped 
   * vertically.
   */ 
  public static void nativeToJavaARGB(int[] pixels, int width, int height) {
    int index = 0;
    int yindex = (height - 1) * width;
    for (int y = 0; y < height / 2; y++) {
      if (BIG_ENDIAN) { // RGBA to ARGB
        for (int x = 0; x < width; x++) {
          int temp = pixels[index];
          pixels[index] = (pixels[yindex] & 0xff000000) |
                          ((pixels[yindex] >> 8) & 0x00ffffff);
          pixels[yindex] = (temp & 0xff000000) |
                           ((temp >> 8) & 0x00ffffff);
          index++;
          yindex++;
        }
      } else { // ABGR to ARGB
        for (int x = 0; x < width; x++) {
          int temp = pixels[index];
          pixels[index] = (pixels[yindex] & 0xff000000) |
                          ((pixels[yindex] << 16) & 0xff0000) |
                          (pixels[yindex] & 0xff00) |
                          ((pixels[yindex] >> 16) & 0xff);
          pixels[yindex] = (temp & 0xff000000) |
                           ((temp << 16) & 0xff0000) |
                           (temp & 0xff00) |
                           ((temp >> 16) & 0xff);
          index++;
          yindex++;
        }
      }
      yindex -= width * 2;
    }

    // Flips image
    if ((height % 2) == 1) {
      index = (height / 2) * width;
      if (BIG_ENDIAN) { // RGBA to ARGB
        for (int x = 0; x < width; x++) {
          pixels[index] = (pixels[index] & 0xff000000) |
                          ((pixels[index] >> 8) & 0x00ffffff);
          index++;
        }
      } else { // ABGR to ARGB
        for (int x = 0; x < width; x++) {
          pixels[index] = (pixels[index] & 0xff000000) |
                          ((pixels[index] << 16) & 0xff0000) |
                          (pixels[index] & 0xff00) |
                          ((pixels[index] >> 16) & 0xff);
          index++;
        }
      }
    }
  }  
  
  
  /**
   * Converts input native OpenGL value (RGBA on big endian, ABGR on little 
   * endian) to Java RGB, so that the alpha component of the result is set
   * to opaque (255).
   */   
  public static int nativeToJavaRGB(int color) {
    if (BIG_ENDIAN) { // RGBA to ARGB
      return ((color << 8) & 0xffffff00) | 0xff;
    } else { // ABGR to ARGB
       return 0xff000000 | ((color << 16) & 0xff0000) |
                           (color & 0xff00) |
                           ((color >> 16) & 0xff);
    }
  }
  
  
  /**
   * Converts input array of native OpenGL values (RGBA on big endian, ABGR on little
   * endian) representing an image of width x height resolution to Java RGB,
   * so that the alpha component of all pixels is set to opaque (255). It also 
   * rearranges the elements in the array so that the image is flipped vertically.
   */   
  public static void nativeToJavaRGB(int[] pixels, int width, int height) {
    int index = 0;
    int yindex = (height - 1) * width;
    for (int y = 0; y < height / 2; y++) {
      if (BIG_ENDIAN) { // RGBA to ARGB
        for (int x = 0; x < width; x++) {
          int temp = pixels[index];
          pixels[index] = 0xff000000 | ((pixels[yindex] >> 8) & 0x00ffffff);
          pixels[yindex] = 0xff000000 | ((temp >> 8) & 0x00ffffff);
          index++;
          yindex++;
        }
      } else { // ABGR to ARGB
        for (int x = 0; x < width; x++) {
          int temp = pixels[index];
          pixels[index] = 0xff000000 | ((pixels[yindex] << 16) & 0xff0000) |
                                       (pixels[yindex] & 0xff00) |
                                       ((pixels[yindex] >> 16) & 0xff);
          pixels[yindex] = 0xff000000 | ((temp << 16) & 0xff0000) |
                                        (temp & 0xff00) |
                                        ((temp >> 16) & 0xff);
          index++;
          yindex++;
        }
      }
      yindex -= width * 2;
    }

    // Flips image
    if ((height % 2) == 1) {
      index = (height / 2) * width;
      if (BIG_ENDIAN) { // RGBA to ARGB
        for (int x = 0; x < width; x++) {
          pixels[index] = 0xff000000 | ((pixels[index] >> 8) & 0x00ffffff);
          index++;
        }
      } else { // ABGR to ARGB
        for (int x = 0; x < width; x++) {
          pixels[index] = 0xff000000 | ((pixels[index] << 16) & 0xff0000) |
                                       (pixels[index] & 0xff00) |
                                       ((pixels[index] >> 16) & 0xff);
          index++;
        }
      }
    }
  }

  
  /**
   * Converts input Java ARGB value to native OpenGL format (RGBA on big endian,
   * BGRA on little endian).
   */
  public static int javaToNativeARGB(int color) {
    if (BIG_ENDIAN) { // ARGB to RGBA
      return ((color >> 24) & 0xff) | 
             ((color << 8) & 0xffffff00);
    } else { // ARGB to ABGR
      return (color & 0xff000000) | 
             ((color << 16) & 0xff0000) | 
             (color & 0xff00) | 
             ((color >> 16) & 0xff);
    }
  }  
  
  
  /**
   * Converts input array of Java ARGB values representing an image of width x height
   * resolution to native OpenGL format (RGBA on big endian, BGRA on little endian).
   * It also rearranges the elements in the array so that the image is flipped 
   * vertically.
   */  
  public static void javaToNativeARGB(int[] pixels, int width, int height) {
    int index = 0;
    int yindex = (height - 1) * width;
    for (int y = 0; y < height / 2; y++) {
      if (BIG_ENDIAN) { // ARGB to RGBA
        for (int x = 0; x < width; x++) {
          int temp = pixels[index];
          pixels[index] = ((pixels[yindex] >> 24) & 0xff) |
                          ((pixels[yindex] << 8) & 0xffffff00);
          pixels[yindex] = ((temp >> 24) & 0xff) |
                           ((temp << 8) & 0xffffff00);
          index++;
          yindex++;
        }

      } else { // ARGB to ABGR
        for (int x = 0; x < width; x++) {
          int temp = pixels[index];
          pixels[index] = (pixels[yindex] & 0xff000000) |
                          ((pixels[yindex] << 16) & 0xff0000) |
                          (pixels[yindex] & 0xff00) |
                          ((pixels[yindex] >> 16) & 0xff);
          pixels[yindex] = (pixels[yindex] & 0xff000000) |
                           ((temp << 16) & 0xff0000) |
                           (temp & 0xff00) |
                           ((temp >> 16) & 0xff);
          index++;
          yindex++;
        }
      }
      yindex -= width * 2;
    }

    // Flips image
    if ((height % 2) == 1) {
      index = (height / 2) * width;
      if (BIG_ENDIAN) { // ARGB to RGBA
        for (int x = 0; x < width; x++) {
          pixels[index] = ((pixels[index] >> 24) & 0xff) |
                          ((pixels[index] << 8) & 0xffffff00);
          index++;
        }
      } else { // ARGB to ABGR
        for (int x = 0; x < width; x++) {
          pixels[index] = (pixels[index] & 0xff000000) |
                          ((pixels[index] << 16) & 0xff0000) |
                          (pixels[index] & 0xff00) |
                          ((pixels[index] >> 16) & 0xff);
          index++;
        }
      }
    }
  }
  
  
  /**
   * Converts input Java ARGB value to native OpenGL format (RGBA on big endian,
   * BGRA on little endian), setting alpha component to opaque (255).
   */  
  public static int javaToNativeRGB(int color) {
    if (BIG_ENDIAN) { // ARGB to RGBA
        return ((color << 8) & 0xffffff00) | 0xff;
    } else { // ARGB to ABGR
        return 0xff000000 | ((color << 16) & 0xff0000) |
                            (color & 0xff00) |
                            ((color >> 16) & 0xff);
    }    
  }    

  
  /**
   * Converts input array of Java ARGB values representing an image of width x height
   * resolution to native OpenGL format (RGBA on big endian, BGRA on little endian),
   * while setting alpha component of all pixels to opaque (255). It also rearranges 
   * the elements in the array so that the image is flipped vertically.
   */ 
  public static void javaToNativeRGB(int[] pixels, int width, int height) {
    int index = 0;
    int yindex = (height - 1) * width;
    for (int y = 0; y < height / 2; y++) {
      if (BIG_ENDIAN) { // ARGB to RGBA        
        for (int x = 0; x < width; x++) {
          int temp = pixels[index];
          pixels[index] = ((pixels[yindex] << 8) & 0xffffff00) | 0xff;
          pixels[yindex] = ((temp << 8) & 0xffffff00) | 0xff;
          index++;
          yindex++;
        }

      } else {        
        for (int x = 0; x < width; x++) { // ARGB to ABGR
          int temp = pixels[index];
          pixels[index] = 0xff000000 | ((pixels[yindex] << 16) & 0xff0000) |
                                       (pixels[yindex] & 0xff00) |
                                       ((pixels[yindex] >> 16) & 0xff);
          pixels[yindex] = 0xff000000 | ((temp << 16) & 0xff0000) |
                                        (temp & 0xff00) |
                                        ((temp >> 16) & 0xff);
          index++;
          yindex++;
        }
      }
      yindex -= width * 2;
    }

    // Flips image
    if ((height % 2) == 1) { // ARGB to RGBA 
      index = (height / 2) * width;
      if (BIG_ENDIAN) {
        for (int x = 0; x < width; x++) {
          pixels[index] = ((pixels[index] << 8) & 0xffffff00) | 0xff;
          index++;
        }
      } else { // ARGB to ABGR
        for (int x = 0; x < width; x++) {
          pixels[index] = 0xff000000 | ((pixels[index] << 16) & 0xff0000) |
                                       (pixels[index] & 0xff00) |
                                       ((pixels[index] >> 16) & 0xff);
          index++;
        }
      }
    }
  }


  public int createShader(int shaderType, String source) {
    int shader = glCreateShader(shaderType);
    if (shader != 0) {
      glShaderSource(shader, source);
      glCompileShader(shader);
      int[] compiled = new int[1];
      glGetShaderiv(shader, GL_COMPILE_STATUS, compiled, 0);
      if (compiled[0] == GL_FALSE) {
        System.err.println("Could not compile shader " + shaderType + ":");
        System.err.println(glGetShaderInfoLog(shader));
        glDeleteShader(shader);
        shader = 0;
      }
    }
    return shader;
  }


  public int createProgram(int vertexShader, int fragmentShader) {
    int program = glCreateProgram();
    if (program != 0) {
      glAttachShader(program, vertexShader);
      glAttachShader(program, fragmentShader);
      glLinkProgram(program);
      int[] linked = new int[1];
      glGetProgramiv(program, GL_LINK_STATUS, linked, 0);
      if (linked[0] == GL_FALSE) {
        System.err.println("Could not link program: ");
        System.err.println(glGetProgramInfoLog(program));
        glDeleteProgram(program);
        program = 0;
      }
    }
    return program;
  }


  public boolean validateFramebuffer() {
    int status = glCheckFramebufferStatus(GL_FRAMEBUFFER);
    if (status == GL_FRAMEBUFFER_COMPLETE) {
      return true;
    } else if (status == GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT) {
      throw new RuntimeException("PFramebuffer: GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT (" + Integer.toHexString(status) + ")");
    } else if (status == GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT) {
      throw new RuntimeException("PFramebuffer: GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT (" + Integer.toHexString(status) + ")");
    } else if (status == GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS) {
      throw new RuntimeException("PFramebuffer: GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS (" + Integer.toHexString(status) + ")");
    } else if (status == GL_FRAMEBUFFER_INCOMPLETE_FORMATS) {
      throw new RuntimeException("PFramebuffer: GL_FRAMEBUFFER_INCOMPLETE_FORMATS (" + Integer.toHexString(status) + ")");
    } else if (status == GL_FRAMEBUFFER_UNSUPPORTED) {
      throw new RuntimeException("PFramebuffer: GL_FRAMEBUFFER_UNSUPPORTED" + Integer.toHexString(status));
    } else {
      throw new RuntimeException("PFramebuffer: unknown framebuffer error (" + Integer.toHexString(status) + ")");
    }
  }


  public static ByteBuffer allocateDirectByteBuffer(int size) {
    return ByteBuffer.allocateDirect(size * SIZEOF_BYTE).order(ByteOrder.nativeOrder());
  }  
  
  
  public static IntBuffer allocateDirectIntBuffer(int size) {
    return ByteBuffer.allocateDirect(size * SIZEOF_INT).order(ByteOrder.nativeOrder()).asIntBuffer();
  }  
  
  
  public static FloatBuffer allocateDirectFloatBuffer(int size) {
    return ByteBuffer.allocateDirect(size * SIZEOF_FLOAT).order(ByteOrder.nativeOrder()).asFloatBuffer();
  }
  
  
  ///////////////////////////////////////////////////////////////////////////////////

  // Java specific stuff

  
  protected class PGLListener implements GLEventListener {
    @Override
    public void display(GLAutoDrawable adrawable) {
      drawable = adrawable;
      context = adrawable.getContext();
      gl = context.getGL();
      gl2 = gl.getGL2ES2();
      try {
        gl2x = gl.getGL2();
      } catch (javax.media.opengl.GLException e) {
        gl2x = null;
      }

      pg.parent.handleDraw();
    }

    @Override
    public void dispose(GLAutoDrawable adrawable) {
    }

    @Override
    public void init(GLAutoDrawable adrawable) {
      drawable = adrawable;
      context = adrawable.getContext();
    }

    @Override
    public void reshape(GLAutoDrawable adrawable, int x, int y, int w, int h) {      
      drawable = adrawable;
      context = adrawable.getContext();      

      if (glColorFboID[0] != 0) {
        // The screen FBO hack needs the FBO to be recreated when starting
        // and after resizing.
        glColorFboID[0] = 0;
      }      
    }
  }


  /** Animator subclass to drive render loop when using NEWT.
   **/
  protected static class PGLAnimator extends AnimatorBase {
    private static int count = 0;
    private Timer timer = null;
    private TimerTask task = null;
    private volatile boolean shouldRun;

    protected String getBaseName(String prefix) {
      return prefix + "PGLAnimator";
    }

    /** Creates an CustomAnimator with an initial drawable to
     * animate.
     */
    public PGLAnimator(GLAutoDrawable drawable) {
      if (drawable != null) {
        add(drawable);
      }
    }

    public synchronized void requestDisplay() {
      shouldRun = true;
    }

    public final boolean isStarted() {
      stateSync.lock();
      try {
        return (timer != null);
      } finally {
        stateSync.unlock();
      }
    }

    public final boolean isAnimating() {
      stateSync.lock();
      try {
        return (timer != null) && (task != null);
      } finally {
        stateSync.unlock();
      }
    }

    private void startTask() {
      if(null != task) {
        return;
      }

      task = new TimerTask() {
        private boolean firstRun = true;
        public void run() {
          if (firstRun) {
            Thread.currentThread().setName("PGL-RenderQueue-" + count);
            firstRun = false;
            count++;
          }
          if (PGLAnimator.this.shouldRun) {
            PGLAnimator.this.animThread = Thread.currentThread();
            // display impl. uses synchronized block on the animator instance
            display();
            synchronized (this) {
              // done with current frame.
              shouldRun = false;
            }
          }
        }
      };

      fpsCounter.resetFPSCounter();
      shouldRun = false;

      timer.schedule(task, 0, 1);
    }

    public synchronized boolean  start() {
      if (timer != null) {
        return false;
      }
      stateSync.lock();
      try {
        timer = new Timer();
        startTask();
      } finally {
        stateSync.unlock();
      }
      return true;
    }

    /** Stops this CustomAnimator. */
    public synchronized boolean stop() {
      if (timer == null) {
        return false;
      }
      stateSync.lock();
      try {
        shouldRun = false;
        if(null != task) {
          task.cancel();
          task = null;
        }
        if(null != timer) {
          timer.cancel();
          timer = null;
        }
        animThread = null;
        try {
          Thread.sleep(20); // ~ 1/60 hz wait, since we can't ctrl stopped threads
        } catch (InterruptedException e) { }
      } finally {
        stateSync.unlock();
      }
      return true;
    }

    public final boolean isPaused() { return false; }
    public synchronized boolean resume() { return false; }
    public synchronized boolean pause() { return false; }
  }
}