package org.processmining.plugins.inductiveVisualMiner.animation.renderingthread.opengleventlistener;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.processmining.plugins.inductiveVisualMiner.animation.renderingthread.ExternalSettingsManager.ExternalSettings;
import org.processmining.visualisation3d.GraphicsPipeline;
import org.processmining.visualisation3d.gldatastructures.JoglShader;
import org.processmining.visualisation3d.maths.JoglVectord2;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.fixedfunc.GLMatrixFunc;

public class RendererImplOpenGLEventListenerInstancedArrays implements GLEventListener {

	private GraphicsPipeline pipeLine = new GraphicsPipeline();
	private JoglShader shader = null;

	private ExternalSettings settings;
	private double time;
	private int width;
	private int height;

	private IntBuffer vertexArrayObject;

	public void init(GLAutoDrawable drawable) {
		System.out.println(" GL listener init");
		drawable.getContext().makeCurrent();
		GL2 gl = drawable.getGL().getGL2();

		pipeLine.gl = gl;

		gl.glEnable(GL.GL_BLEND);
		gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
		gl.glDisable(GL2.GL_CULL_FACE);

		//set up the shaders
		{
			shader = new JoglShader(
					"/org/processmining/plugins/inductiveVisualMiner/animation/renderingthread/opengltries",
					"vaoOtherShaderVSinstancedArrays", "vaoOtherShaderFSinstancedArrays");
			shader.Create(pipeLine);
			shader.Bind(pipeLine);
		}

		//set up quad to draw the circles on later
		{

			//define the quad
			float[] quadVertices = { //
					//position		//colour
					-0.05f, -0.05f, 1, 1, 0, // Left  
					0.05f, -0.05f, 1, 0, 1, // Right 
					0.0f, 0.05f, 0, 1, 1 // Top   
			};
			FloatBuffer quadVerticesBuffer = FloatBuffer.wrap(quadVertices);

			//define the translations
			FloatBuffer translations = FloatBuffer.allocate(200);
			for (int y = -10; y < 10; y += 2) {
				for (int x = -10; x < 10; x += 2) {
					translations.put(x / 10f);
					translations.put(y / 10f);
				}
			}
			translations.rewind();

			//set up a vertex array object
			vertexArrayObject = IntBuffer.allocate(1);
			gl.glGenVertexArrays(1, vertexArrayObject);

			//create a vertex buffer
			IntBuffer vertexBufferObject = IntBuffer.allocate(1);
			gl.glGenBuffers(1, vertexBufferObject);

			//create an instanced array
			IntBuffer instanceVertexBufferObject = IntBuffer.allocate(1);
			gl.glGenBuffers(1, instanceVertexBufferObject);

			//bind the vertex array
			gl.glBindVertexArray(vertexArrayObject.get(0));
			{
				//bind and send the vertex buffer in the vertex array
				gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, vertexBufferObject.get(0));
				gl.glBufferData(GL2.GL_ARRAY_BUFFER, quadVertices.length * 4, quadVerticesBuffer, GL2.GL_STATIC_DRAW);

				//tell the structure of the float buffer
				gl.glEnableVertexAttribArray(0);
				gl.glVertexAttribPointer(0, 2, GL2.GL_FLOAT, false, 5 * 4, 0); //position

				gl.glEnableVertexAttribArray(1);
				gl.glVertexAttribPointer(1, 3, GL2.GL_FLOAT, false, 5 * 4, 2 * 4); //colour

				//bind and send the instanced array
				gl.glBindBuffer(GL.GL_ARRAY_BUFFER, instanceVertexBufferObject.get(0));
				gl.glBufferData(GL.GL_ARRAY_BUFFER, 200 * 4, translations, GL.GL_STATIC_DRAW);

				//tell the structure of the instanced array
				gl.glEnableVertexAttribArray(2);
				gl.glVertexAttribPointer(2, 2, GL.GL_FLOAT, false, 0, 0); //offset

				//unbind the vertex buffer
				gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, 0);

				gl.glVertexAttribDivisor(2, 1);
			}

			//unbind the vertex array
			gl.glBindVertexArray(0);
		}

	}

	public void dispose(GLAutoDrawable drawable) {
		shader.Unbind(pipeLine);
		shader.Dispose(pipeLine);
		System.out.println(" GL listener dispose");
	}

	public void display(GLAutoDrawable drawable) {
		if (settings != null && settings.filteredLog != null && settings.tokens != null && settings.transform != null) {
			GL2 gl = drawable.getGL().getGL2();

			//clear the previous drawing
			gl.glClearColor(1f, 1f, 0f, 0f);
			gl.glClear(GL.GL_COLOR_BUFFER_BIT);
			gl.glLoadIdentity();

			//gl.glUseProgram(shader.getProgramHandle());
			gl.glBindVertexArray(vertexArrayObject.get(0));
			//gl.glDrawArrays(GL2.GL_TRIANGLES, 0, 3);
			gl.glDrawArraysInstanced(GL2.GL_TRIANGLES, 0, 3, 100);
			gl.glBindVertexArray(0);

			//			gl.glColor4f(1, 0, 0, 1);
			//			gl.glBegin(GL2.GL_TRIANGLES);
			//			gl.glVertex2f(-0.5f, -0.5f);
			//			gl.glVertex2f(0.5f, -0.5f);
			//			gl.glVertex2f(0f, 0.5f);
			//			gl.glEnd();

			//			gl.glBindVertexArray(quadVAO);
			//			gl.glDrawArraysInstanced(GL2.GL_TRIANGLES, 0, 6, 100);  
			//			gl.glBindVertexArray(0);
		}
	}

	public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
		System.out.println(" GL listener reshape");

		GL2 gl = drawable.getGL().getGL2();

		if (height == 0) {
			height = 1; // prevent divide by zero
		}
		double aspectRatio = (double) width / height;
		this.width = width;
		this.height = height;

		// Set the view port (display area) to cover the entire window
		gl.glViewport(0, 0, width, height);

		//tell the graphics card
		shader.SetUniform(pipeLine, "imageSize", new JoglVectord2(width, height));

		// Setup perspective projection, with aspect ratio matches viewport
		gl.glMatrixMode(GLMatrixFunc.GL_PROJECTION); // choose projection matrix
		gl.glLoadIdentity(); // reset projection matrix
	}

	public void setParameters(ExternalSettings settings, double time) {
		this.settings = settings;
		this.time = time;
	}

}
