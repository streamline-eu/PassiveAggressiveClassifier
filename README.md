# WP2-MidtermReport
Datasets:
   * Train: training_set_preprocessed.csv, training_set.csv
      * We used the preprocessed set. The separator is ";". The first line contains the column names (labels: Categorie2, features: stem)
   * Test: validation_set_preprocessed.csv, validation_set.csv

Flink implementation deployment notes
Files and directories:

   * flink-1.2.0 - Flink distribution (can be downloaded from Flink website)
   * im-text-classification - Source-code for text classification with Parameter Server.
   * multi_train_1000.csv - Sample training input.
   * multi_predict_1000.csv - Sample prediction input (i.e. unlabeled vectors).
   * output_model_example.csv - Training output example.
   * prediction_output_example.csv - Prediction output example.
   * training.properties - Training configuration.
   * prediction.properties - Prediction configuration.
   * flink-parameter-server - Source code for Parameter Server. (Also available on GitHub at https://github.com/gaborhermann/flink-parameter-server)
   * im-text-classification-assembly-0.1.0.jar - Text classification bytecode, that can be used with Flink.

How to use it?

1.	Configure training and prediction jobs. (Give input and output paths.)
   * training.conf
   * prediction.conf

2.	Start Flink:
./flink-1.2.0/bin/start-local.sh

3.	Run training:
./flink-1.2.0/bin/flink run -p 4 -c hu.sztaki.ilab.imtextclassification.PassiveAggressiveMultiClassTraining im-text-classification-assembly-0.1.0.jar training.properties

4.	Run prediction:
./flink-1.2.0/bin/flink run -p 4 -c hu.sztaki.ilab.imtextclassification.PassiveAggressivePrediction im-text-classification-assembly-0.1.0.jar prediction.properties


Other notes

To compile, we can use SBT with the two projects:
   * cd flink-parameter-server
   * sbt publish-local
   * cd im-text-classification
   * sbt assembly

This will create the assembly jar at:
im-text-classification/target/im-text-classification-assembly-0.1.0.jar

This Parameter Server is capable of training AND predicting in one job, online.
See the source code for im-text-classification to see how to use it in Scala.
Results:
   * validation set:
      * sklearn passive agressive classifier : 75% accuracy
      * sklearn multinomial naive bayes classifier: 68% accuracy
      * sklearn bernoulli naive bayes: 66% accuracy
      * tensorflow: 70% accuracy
      * tesnorflow logistic regression: 75% accuracy
      * sklearn LinearSVC 75% accuracy
   * using only the training set (9:1): 
      * sklearn passive agressive classifier : 92% accuracy
      * sklearn nultinomial naive bayes classifier: 88% accuracy 
 
ClsTester:
   * Parameters: 
      * data_train: training data. Features and labels should be accesible using data_train[0], data_train[1]
      * data_test: test data. Features and labels should be accesible using data_test['features'], data_test['labels']
      * data_dir: directory for saving/loading samples.
      * results_dir: directory for saving/loading models, tfidf vectorizers, predictions.
   * Attributes:
      * models: classifiers
      * vectorizers: tfidf vectorizers
      * samples: samples for vectorizer/model fitting/testing
      * transformed_datas: samples converted to tfidf and partitioned into smaller batches for partial fit
      * predictions: predictions
   * Methods: 
      * sampling([df=self.data_train, n=200000, column_name='labels', save=False]): 
         * takes a sample from df.
         * n: the maximum number of rows per label
         * save: if not False saves the sample to data_dir+'sample_'+str(save)+'.sample'
         * return: sample in [features, labels] format
      * feature_transform(sample_name, vectorizer_name[, batch_num, pool_num, save=False]): 
         * sample_name: the sample used form self.samples
         * vectorizer_name: the vectorizers name from self.vectorizers. Used for transforming the sample
         * batch_num: split the sample into batch_num parts for partial fitting.
         * save: if True save the samples to data_dir+'transformed_'+str(sample_name)+'_'str(vectorizer_name)+'.tfidf', otherwise save the sample to data_dir+'transformed_'+str(save)+'.tfidf'
         * return: [features_split, labels_split] where features split and labels_split are arrays with batch_num length.
      * fit_vectorizer(vectorizer_name, sample_name[, save=True]) 
         * fits the self.vectorizers[vectorizer_name] using self.samples[sample_name]
         * save: if True save the vectorizer to self.results_dir+'vectorizer_'+str(vectorizer_name)+'.vec'. If not True or False save it to self.results_dir+'vectorizer_'+str(save)+'.vec'
      * fit_test_model(model_name, data_name, vectorizer_name[, save_model=True, save_prediciton=False]) 
         * fits self.models[model_name] using self.tranformed_datas[data_name]
         * tests the model on self.data_test after tfidf transforming it using self.vectorizers[vectorizer_name]
         * the prediction and transformed test can be reached as self.predictions[model_name] and self.transformed_datas[vectorizer_name+'_test']
         * prints the accuracy score
      * save_vectorizer(name)/save_sample(name)/save_transformed_data(name)/save_model(name)/save_prediction(name): 
         * saves the object self.vectorizers[name], self.samples[name] etc.
      * load_vectorizer(name)/load_sample(name)/load_transformed_data(name)/load_model(name)/load_prediction(name): 
         * loads the object into self.vectorizers[name], self.samples[name]
    
Example code:
If you just want the tfidf files:

`
from sklearn.externals import joblib
from scipy.sparse import vstack
data=joblib.load(<file_name>)
features=vstack(data[0])
labels=[label for batch in data[1] for label in batch]
`

Converting and training:

`
import sys
sys.path.append("/mnt/idms/rpalovics/InternetMemory/im-text-classification/python/modeling/")
import classifier_tester
data_train=pd.read_csv('/mnt/idms/rpalovics/InternetMemory/data/training_set_preprocessed.csv').rename(columns={'stem':'features', 'Categorie2':'labels'})
data_test=pd.read_csv('/mnt/idms/rpalovics/InternetMemory/data/validation_set_preprocessed.csv').rename(columns={'stem':'features', 'Categorie2':'labels'})
 
ct=classifier_tester.ClsTester(data_test=data_train, data_test=data_test)
ct.samples[1]=ct.sampling(n=200000)
ct.vectorizers['md3']=TfidfVectorizer(min_df=3)
#make the tfidf table from samples[1] using vectorizers['md3'] and split it into a 100 batches.
ct.transformed_datas['1_md3']=feature_transform(1, 'md3', batch_num=100)
ct.models['sgd']=SGDClassifier(...)
#fit models['sgd1'] using transformed_datas['1_md3'] and test it on ct.data_test after converting it to tfidf using vectorizers['md3']
ct.fit_test_model('sgd', 'td1', 'vec1')
result: prints accuracy, saves model, vectorizer and predictions with the original labels
` 
