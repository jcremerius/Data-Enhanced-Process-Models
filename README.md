# Data-Enhanced-Process-Models

The jupyter notebook in this repository creates the event log for the experiment in the paper "Data Enhanced Process Models in Process Mining"
It uses the MIMIC-IV (https://physionet.org/content/mimiciv/1.0/) database, stored in a Postgres database. 
First, all cases related to acute Heart Failure are retrieved. Then, the hospital data for their respective hospital admission is fetched. 
Additionally, X-ray data is retrieved from an external database called MIMIC-CXR (https://physionet.org/content/mimic-cxr/2.0.0/). 
After that, relevant lab values and physician order entries, according to the clinical guideline (https://www.escardio.org/Guidelines/Clinical-Practice-Guidelines/Acute-and-Chronic-Heart-Failure), are used for the event log. 
The databases can be accessed via physionet, which require CITI training for access (https://mimic.mit.edu/iv/). 

1. Execute all function definitions at the bottom of the script. 
2. Execute the script from top to bottom. When the database is stored locally, it should take about an hour.
3. Create XES file from the csv with any tool of your preference. We used Disco.
4. Execute PROM from your preferred java ide (e.g. Eclipse) and use the Inductive Visual Miner. The aggregations are computed automatically. 
Link to the Inductive Visual Miner implementation: https://1drv.ms/u/s!AmDP37g19dJNh-AUBk9xo_T859Imrg?e=XFhcPv 
Link to the Setup of PROM in an IDE: https://svn.win.tue.nl/trac/prom/wiki/Contribute


Inductive Visual Miner Implementation:

The main contribution of this paper can be found in the following file: InductiveVisualMiner/src/org/processmining/plugins/inductiveVisualMiner/visualisation/ProcessTreeVisualisation.java in the function convertActivity.
There, the event attribute aggregations are calculated and attached to the process activity. The HashMap attributesToDisplay includes all activities with their event attribute aggregation. In future work, this array is supposed to be set by the GUI to enable a flexible addition/removal of event attribute aggregations. So far, this has to be set in the code.
